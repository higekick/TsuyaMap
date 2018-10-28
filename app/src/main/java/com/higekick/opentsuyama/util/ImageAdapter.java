package com.higekick.opentsuyama.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by User on 2017/08/27.
 */

public class ImageAdapter extends BaseAdapter {
    private static String TAG = ImageAdapter.class.getSimpleName();
    private static final int IMAGE_PADDING = 10;

    private Activity mActivity;
    private List<String> urlList;

    private int screenWidth;
    private int screenHeight;
    private int imageViewWidth;
    private int imageViewHeight;


    public ImageAdapter(Activity a, List<String> list) {
        mActivity = a;
        urlList = list;
        WindowManager wm = (WindowManager)a.getSystemService(WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();

        Point size = new Point();
        disp.getSize(size);

        screenWidth = size.x;
        screenHeight = size.y;

        imageViewWidth = screenWidth / 2;
        imageViewHeight = (int) ((float)imageViewWidth * (2.0/3.0) );
    }

    public int getCount() {
        return urlList.size();
    }

    public String getItem(int position) {
        return urlList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mActivity);
            imageView.setLayoutParams(new GridView.LayoutParams(imageViewWidth, imageViewHeight));
//            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING);
        } else {
            imageView = (ImageView) convertView;
        }

        String url = getItem(position);
        Bitmap bitmap = Util.createGalleryBitmap(url, mActivity);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return imageView;
        } else {
            return null;
        }
//        Picasso
//                .get()
//                .load(url)
//                .placeholder(R.drawable.ic_photo_grey_50_18dp)
//                .resize(imageViewWidth,imageViewHeight)
//                .centerCrop()
//                .error(R.drawable.ic_error_outline_red_300_36dp)
//                .into(imageView);

    }
}
