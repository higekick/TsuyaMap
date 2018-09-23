package com.higekick.opentsuyama.util;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.higekick.opentsuyama.DownloadFragment;
import com.higekick.opentsuyama.R;
import com.higekick.opentsuyama.S3RetrieveJobService;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

/**
 * Created by User on 2016/12/19.
 */

public class Util {

    public static URL getURL(String urlpath){
        URL url=null;
        try {
            url = new URL(urlpath);
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return url;
    }

    public static JSONArray getJsonFromFile(File file, Context con) throws FileNotFoundException, JSONException {
        InputStream is = new FileInputStream(file);
        Writer w = new StringWriter();
        try{
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = r.readLine();
            while (line != null) {
                w.write(line);
                line = r.readLine();
            }
        } catch (Exception ex){
            Log.e("ReadingJson","Fatal Unhandled Exception",ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex){
                Log.e("ReadingJson","Fatal Unhandled Exception",ex);
            }
        }

        return new JSONArray(w.toString());
    }

    // ネットワーク接続確認
    public static boolean netWorkCheck(Context context){
        ConnectivityManager cm =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        boolean result = false;
        if( info != null ){
            result = info.isConnected();
        } else {
            result = false;
        }
        if (result) {
            return true;
        } else {
            Toast.makeText(context, context.getResources().getText(R.string.message_no_network),Toast.LENGTH_LONG).show();
            return false;
        }
    }

    // 新規フォルダを作成し、画像ファイルを保存する
    public static boolean createFolderSaveImage(Bitmap imageToSave, String fileName, Context con) {
        // 新しいフォルダへのパス
        String folderPath = Environment.getExternalStorageDirectory()
                + "/" + con.getResources().getString(R.string.app_name) + "/";
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // NewFolderに保存する画像のパス
        File file = new File(folder, fileName);
        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            // これをしないと、新規フォルダは端末をシャットダウンするまで更新されない
            showFolder(file,con);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // ContentProviderに新しいイメージファイルが作られたことを通知する
    private static void showFolder(File path, Context con) throws Exception {
        try {
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = con.getApplicationContext()
                    .getContentResolver();
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATE_MODIFIED,
                    System.currentTimeMillis() / 1000);
            values.put(MediaStore.Images.Media.SIZE, path.length());
            values.put(MediaStore.Images.Media.TITLE, path.getName());
            values.put(MediaStore.Images.Media.DATA, path.getPath());
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            throw e;
        }
    }

    public static Bitmap createGalleryBitmap(String url, Context context) {
        File file = new File(url);
        Uri uri = Uri.fromFile(file);
        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException ex) {
            Log.e("LoadingImage", "failed to load image.", ex);
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
        return bitmap;
    }

    public static String getJsonDirPath(Context con) {
        return con.getFilesDir().getAbsolutePath() + "/" + Const.JSON_PRFX;
    }

    public static String getImageDirPath(Context con) {
        return con.getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX;
    }

    public static String getDirName(Context context, String dirId, String path){
        String pathDir = context.getFilesDir().getAbsolutePath() + "/" + path + "/" + dirId + "/" + "dirname.txt";
        File f = new File(pathDir);
        if (f.exists()) {

            Uri uri = Uri.fromFile(f);
            InputStream stream;
            try {
                stream = context.getContentResolver().openInputStream(uri);
                InputStreamReader inputStreamReader = new InputStreamReader(stream);
                BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    return line;
                }
            } catch (FileNotFoundException ex) {
                Log.e("LoadingImage", "failed to load image.", ex);
                return null;
            } catch (IOException ex) {
                Log.e("LoadingImage", "failed to load image.", ex);
                return null;
            }
        }
        return "";
    }

    public static void putInvisibleFile(Context context, String dirId, String path) {
       processInvisibleFile(context, dirId, path, true);
    }

    public static void deleteInvisibleFile(Context context, String dirId, String path) {
        processInvisibleFile(context, dirId, path, false);
    }

    public static void processInvisibleFile(Context context, String dirId, String path, boolean isMake) {
        File f = getInvisibleFile(context, dirId, path);
        try {
            if (isMake) {
                f.createNewFile();
            } else {
                if (f.exists()) {
                    f.delete();
                }
            }
        } catch (FileNotFoundException ex) {
            Log.e("LoadingImage", "failed to load image.", ex);
        } catch (IOException ex) {
            Log.e("LoadingImage", "failed to load image.", ex);
        }
    }

    public static File getInvisibleFile(Context context, String dirId, String path) {
        String pathDir = context.getFilesDir().getAbsolutePath() + "/" + path + "/" + dirId + "/" + "invisible";
        File f = new File(pathDir);
        return f;
    }

    public static void startService(Context context, String s3Path){
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(S3RetrieveJobService.EXTRAS_S3_PATH, s3Path);
        JobInfo info = new JobInfo.Builder(1,new ComponentName(context, S3RetrieveJobService.class))
                .setMinimumLatency(0) // 0～5秒の間に動かす
                .setOverrideDeadline(5000)
                .setExtras(bundle) // JobParamsセット
                .build();
        jobScheduler.schedule(info);
    }

    public static void setPreferenceValue(Context context, String key, Object value){
        SharedPreferences data = context.getSharedPreferences(Const.NAME_PREFERRENCE_FILE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        if (value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        }
        else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        }
        editor.apply();
    }

    public static int getIntPreferenceValue(Context context, String key){
        SharedPreferences data = context.getSharedPreferences(Const.NAME_PREFERRENCE_FILE,Context.MODE_PRIVATE);
        return data.getInt(key, 0);
    }

    public static boolean getBooleanPreferenceValue(Context context, String key){
        SharedPreferences data = context.getSharedPreferences(Const.NAME_PREFERRENCE_FILE,Context.MODE_PRIVATE);
        return data.getBoolean(key, false);
    }

    public static int getFileCount(Context context, String dirId, String path) {
        String pathDir = context.getFilesDir().getAbsolutePath() + "/" + path + "/" + dirId;
        File f = new File(pathDir);
        if (f == null) {
            return 0;
        }
        File[] list = f.listFiles();
        int count = 0;
        if (list == null) {
            return 0;
        }
        for (File f2 : list) {
            if (f2.isFile() &&
                    ( f2.getName().endsWith(".jpg") || f2.getName().endsWith(".json") )
                    ){
                count++;
            }
        }
        return count;
    }
}
