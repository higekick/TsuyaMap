package com.higekick.opentsuyama;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

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
import com.higekick.opentsuyama.util.Util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class S3RetrieveJobService extends JobService {
    public final static String TAG = S3RetrieveJobService.class.getSimpleName();
    public static final String BUCKET_NAME = "tsuyama-open";

    // params
    public final static String EXTRAS_S3_PATH = "S3_PATH";

    // background worker
    HandlerThread mHandlerThread;
    Handler mHandler;

    // notification for download
    NotificationManager mNofificationManager;
    NotificationCompat.Builder mNotificationBuilder;

    private Integer mSizeOfFile;
    private AtomicInteger mIndexOfFile;
    private String mS3Path; // json or jpg

    static boolean isRunning = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        if (isRunning) {
            return false;
        } else {
            isRunning = true;
        }
        mHandlerThread = new HandlerThread("S3RetrieveJobThread", THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mS3Path = params.getExtras().getString(EXTRAS_S3_PATH);
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
        isRunning = false;
        return false;
    }

    private void execute(){
        // AWS mobile client initialize
        AWSMobileClient.getInstance().initialize(this).execute();
        AWSCredentialsProvider provider = AWSMobileClient.getInstance().getCredentialsProvider();
        AmazonS3Client s3Client = new AmazonS3Client(provider);
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(this)
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(s3Client)
                        .build();

        // create notification channel for above oreo
        mNofificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        mNotificationBuilder = new NotificationCompat.Builder(this, Const.ID_CHANNEL_DOWNLOAD)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("ダウンロード中...")
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.tsuyama_logo_pinkbk);

        try {
            retrieveImage(s3Client, transferUtility, this);
        } catch (Exception ex) {
            Toast.makeText(this, R.string.message_error_network, Toast.LENGTH_LONG);
        }
    }

    private void retrieveImage(AmazonS3Client c, TransferUtility t, Context con){
        // create parent img directory
        File imgDir = new File(con.getFilesDir().getAbsolutePath() + "/" + mS3Path);
        if (imgDir.exists()) {
            imgDir.delete();
            Log.d(TAG, "img directory deleted;");
        } else {
            imgDir.mkdir();
            Log.d(TAG, "img directory created;");
        }

        // download from S3
        ObjectListing list = c.listObjects(BUCKET_NAME, mS3Path);
        List<S3ObjectSummary> obsmry = list.getObjectSummaries();
        URLCodec codec = new URLCodec("UTF-8");

        mIndexOfFile = new AtomicInteger(0);
        mSizeOfFile = obsmry.size();
        sendBroadcastMax();
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
                String savePath = con.getFilesDir().getAbsolutePath() + "/" + mS3Path + "/" + dirName + "/" + fileName;
                Log.d(TAG, savePath);
                downloadFileFromS3(t, key, savePath);
            } catch (AmazonS3Exception ex) {
                Log.e(TAG, "error!", ex);
            }
        }
    }

    private synchronized void observeDownloadAndBroadCastFinish() {
        mNotificationBuilder.setProgress(mSizeOfFile, mIndexOfFile.get(), false);
        mNotificationBuilder.setContentText("ダウンロード中..." + mIndexOfFile + "/" + mSizeOfFile);
        mNofificationManager.notify(1, mNotificationBuilder.build());
        sendBroadcastProgress();
       if (mIndexOfFile.get() >= mSizeOfFile) {
           Intent intent = new Intent(Const.ACTION_RETRIEVE_FINISH);
           Log.d(TAG, "Download finish and send broadcast");
           sendBroadcast(intent);
           sendBroadcastFinish();

           isRunning = false;
           Util.setPreferenceValue(this, Const.KEY_DOWNLOAD_X + mS3Path, true);

           Intent intentMain = new Intent(this,MainActivity.class);
           PendingIntent pi = PendingIntent.getActivity(this, 0, intentMain, FLAG_UPDATE_CURRENT);
           Notification notification = mNotificationBuilder
                   .setContentTitle(getResources().getString(R.string.app_name))
                   .setContentText("ダウンロード完了")
                   .setSmallIcon(R.drawable.tsuyama_logo_pinkbk)
                   .setContentIntent(pi)
                   .setAutoCancel(true)
                   .build();
           mNofificationManager.notify(1,notification);
       }
    }

    private void sendBroadcastProgress(){
        Intent i = new Intent(Const.ACTION_RETRIEVE_PROGRESS);
        Bundle bundle = new Bundle();
        bundle.putInt("progress", mIndexOfFile.get());
        i.putExtras(bundle);
        sendBroadcast(i);
    }

    private void sendBroadcastMax(){
        Intent i = new Intent(Const.ACTION_RETRIEVE_PROGRESS);
        Bundle bundle = new Bundle();
        bundle.putInt("max", mSizeOfFile);
        i.putExtras(bundle);
        sendBroadcast(i);
    }

    private void sendBroadcastFinish(){
        Intent i = new Intent(Const.ACTION_RETRIEVE_PROGRESS);
        Bundle bundle = new Bundle();
        bundle.putBoolean("finish", true);
        i.putExtras(bundle);
        sendBroadcast(i);
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

    private String makeDir(String key, Context con){
        String[] split = key.split("/");
        if (split.length != 3) {
            // こんな形式なので
            // jpg/津山市_ごんごまつり花火_画像/tsuyamashigongomatsurihanabigazou720170203touroku.jpg
            return null;
        }
        String dirName=con.getFilesDir().getAbsolutePath() + "/" + mS3Path + "/"+ split[1];
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
            NotificationChannel channel = new NotificationChannel(Const.ID_CHANNEL_DOWNLOAD, name, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            mNofificationManager.createNotificationChannel(channel);
        }
    }

}
