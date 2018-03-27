package com.higekick.opentsuyama.util;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.higekick.opentsuyama.R;

import java.io.BufferedReader;
import java.io.File;
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

    public static String getJsonFromRawFile(int resId, Context con) {
        InputStream is = con.getResources().openRawResource(resId);
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

        return w.toString();
    }

    // ネットワーク接続確認
    public static boolean netWorkCheck(Context context){
        ConnectivityManager cm =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if( info != null ){
            return info.isConnected();
        } else {
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




}
