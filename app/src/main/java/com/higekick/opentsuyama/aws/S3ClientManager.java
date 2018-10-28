package com.higekick.opentsuyama.aws;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.higekick.opentsuyama.util.Const;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.higekick.opentsuyama.util.Const.IMG_PRFX;

public class S3ClientManager {
    public final static String TAG = S3ClientManager.class.getSimpleName();

    public static final String BUCKET_NAME = "tsuyama-open";

    public static void initAWSClient(final Context context){

        new AsyncTask<Void, Void, Void>() {
            WeakReference<Context> weakReference = new WeakReference(context);
            @Override
            protected Void doInBackground(Void... voids) {
                AWSMobileClient.getInstance().initialize(weakReference.get()).execute();

                AWSCredentialsProvider provider = AWSMobileClient.getInstance().getCredentialsProvider();
                AmazonS3Client s3Client = new AmazonS3Client(provider);
                TransferUtility transferUtility =
                        TransferUtility.builder()
                                .context(context)
                                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                                .s3Client(s3Client)
                                .build();

                retrieveImage(s3Client, transferUtility, context);

                return null;
            }
        }.execute();
    }

    private static void retrieveImage(AmazonS3Client c, TransferUtility t, Context con){
        // create parent img directory
        File imgDir = new File(con.getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX);
        if (imgDir.exists()) {
            imgDir.delete();
            Log.d(TAG, "img directory deleted;");
        } else {
            imgDir.mkdir();
            Log.d(TAG, "img directory created;");
        }

        // download from S3
        ObjectListing list = c.listObjects(BUCKET_NAME, Const.IMG_PRFX);
        List<S3ObjectSummary> obsmry = list.getObjectSummaries();
        URLCodec codec = new URLCodec("UTF-8");
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
//            InputStream is = obj.getObjectContent();
//            File saveFile = new File(savePath);
//            FileOutputStream output = null;
//            try {
//                output = new FileOutputStream(saveFile.getAbsolutePath());
//                byte buf[]=new byte[256];
//                int len;
//                while((len=is.read(buf))!=-1){
//                    output.write(buf,0,len);
//                }
//            }catch (Exception ex){
//                Log.e(TAG, "Error! Downloading from S3", ex);
//            }finally {
//                try {
//                    output.flush();
//                    output.close();
//                    is.close();
//                } catch (Exception ex){}
//            }
        }
    }

    public static void downloadFileFromS3(TransferUtility t, String key, String path){
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
}
