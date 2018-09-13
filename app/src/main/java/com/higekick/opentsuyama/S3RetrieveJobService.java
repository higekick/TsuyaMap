package com.higekick.opentsuyama;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.higekick.opentsuyama.util.Const;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.higekick.opentsuyama.util.Const.IMG_PRFX;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class S3RetrieveJobService extends JobService {
    public final static String TAG = S3RetrieveJobService.class.getSimpleName();
    public static final String BUCKET_NAME = "tsuyama-open";

    HandlerThread mHandlerThread;
    Handler mHandler;

    NotificationManager mNofificationManager;
    NotificationCompat.Builder mNotificationBuilder;

    private Integer mSizeOfFile;
    private AtomicInteger mIndexOfFile;

    @Override
    public boolean onStartJob(JobParameters params) {
        mHandlerThread = new HandlerThread("S3RetrieveJobThread", THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
               execute();
            }
        });
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mHandlerThread!=null) {
            mHandlerThread.quit();
        }
        return false;
    }

    private void execute(){
        AWSMobileClient.getInstance().initialize(this).execute();

        AWSCredentialsProvider provider = AWSMobileClient.getInstance().getCredentialsProvider();
        AmazonS3Client s3Client = new AmazonS3Client(provider);
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(this)
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();

        mNofificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        mNotificationBuilder = new NotificationCompat.Builder(this, Const.ID_CHANNEL_DOWNLOAD)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("ダウンロード中...")
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.tsuyama_logo_pinkbk);

        retrieveImage(s3Client, transferUtility, this);
    }

    private void retrieveImage(AmazonS3Client c, TransferUtility t, Context con){
        // create parent img directory
        File imgDir = new File(con.getFilesDir().getAbsolutePath() + "/" + IMG_PRFX);
        if (imgDir.exists()) {
            imgDir.delete();
            Log.d(TAG, "img directory deleted;");
        } else {
            imgDir.mkdir();
            Log.d(TAG, "img directory created;");
        }

        // download from S3
        ObjectListing list = c.listObjects(BUCKET_NAME, IMG_PRFX);
        List<S3ObjectSummary> obsmry = list.getObjectSummaries();
        URLCodec codec = new URLCodec("UTF-8");

        mIndexOfFile = new AtomicInteger(0);
        mSizeOfFile = obsmry.size();
        mNotificationBuilder.setProgress(mSizeOfFile, 0, false);
        mNofificationManager.notify(1, mNotificationBuilder.build());

        for(S3ObjectSummary s : obsmry){
            String key = s.getKey();
            Log.d(TAG, key);
            String dirName = makeDir(key,con);
            String fileName = concatFileName(key);
            if (dirName == null || fileName == null) {
                continue;
            }

            String encodedResult = "";
            try {
                encodedResult = codec.encode(key, "UTF-8");
                Log.d(TAG, "エンコード結果:" + encodedResult);
                String decodedResult = codec.decode(encodedResult, "UTF-8");
                Log.d(TAG,"デコード結果:" + decodedResult);
            } catch (UnsupportedEncodingException ex) {
            } catch (DecoderException ex) {
            }
            try {
//                S3Object obj = c.getObject(s.getBucketName(), encodedResult);
                String savePath = con.getFilesDir().getAbsolutePath() + "/" + IMG_PRFX + "/" + dirName + "/" + fileName;
                Log.d(TAG, savePath);
                downloadFileFromS3(t, key, savePath);
            } catch (AmazonS3Exception ex) {
                Log.e(TAG, "error!", ex);
            }
        }
    }

    private synchronized void observeDownloadAndBroadCastFinish() {
        mNotificationBuilder.setProgress(mSizeOfFile, mIndexOfFile.get(), false);
       if (mIndexOfFile.get() >= mSizeOfFile) {
           Intent intent = new Intent(Const.ACTION_RETRIEVE_FINISH);
           Log.d(TAG, "Download finish and send broadcast");
           sendBroadcast(intent);
           Notification notification = mNotificationBuilder
                   .setContentTitle(getResources().getString(R.string.app_name))
                   .setContentText("ダウンロード完了")
                   .setSmallIcon(R.drawable.tsuyama_logo_pinkbk)
                   .setAutoCancel(true)
                   .build();
           mNofificationManager.notify(1,notification);
       }
    }

    public void downloadFileFromS3(TransferUtility t, String key, String path){
        File f = new File(path);
        TransferObserver transferObserver = t.download(
                BUCKET_NAME, /* The bucket to download from */
                key,         /* The key for the object to download */
                f            /* The file to download the object to */
        );

        transferObserver.setTransferListener(new TransferListener(){
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "State Change" + state);
                if (state == TransferState.COMPLETED ||
                        state == TransferState.CANCELED ||
                        state == TransferState.FAILED) {
                    mIndexOfFile.incrementAndGet();
                }
                observeDownloadAndBroadCastFinish();
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                try {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                    Log.d(TAG, "Progress in %" + percentage);
                }catch (Exception ex){
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "ERROR. id="+ id, ex);
            }

        });
    }

    private static String makeDir(String key, Context con){
        String[] split = key.split("/");
        if (split.length != 3) {
            // こんな形式なので
            // jpg/津山市_ごんごまつり花火_画像/tsuyamashigongomatsurihanabigazou720170203touroku.jpg
            return null;
        }
        String dirName=con.getFilesDir().getAbsolutePath() + "/" + IMG_PRFX + "/"+ split[1];
        File dir = new File(dirName);
        if (!dir.exists()) {
            boolean r = dir.mkdir();
            if (r) {
                Log.d(TAG, "made directory:" + dirName);
            }
        }
        return split[1];
    }

    private static String concatFileName(String key){
        String[] split = key.split("/");
        if (split.length != 3) {
            // こんな形式なので
            // jpg/津山市_ごんごまつり花火_画像/tsuyamashigongomatsurihanabigazou720170203touroku.jpg
            return null;
        }
        String fileName=split[2];
        return fileName;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        String name = "ダウンロードに関する通知";
        String description = "地図情報や写真ファイルのダウンロードに関する通知です";

        if (mNofificationManager.getNotificationChannel(Const.ID_CHANNEL_DOWNLOAD) == null) {
            NotificationChannel channel = new NotificationChannel(Const.ID_CHANNEL_DOWNLOAD, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            mNofificationManager.createNotificationChannel(channel);
        }
    }

}
