package com.higekick.opentsuyama;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class EntranceGalleryFragment extends Fragment {


    public EntranceGalleryFragment() {
        // Required empty public constructor
    }

    RecyclerView rcycleView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_entrance_gallery, container, false);
        rcycleView = view.findViewById(R.id.recyclerView);
        rcycleView.setHasFixedSize(true);
        RecyclerView.LayoutManager mgr = new GridLayoutManager(getContext(), 2);
        rcycleView.setLayoutManager(mgr);
        MyAdapter adapter = new MyAdapter(getContext(), getListItem());
        rcycleView.setAdapter(adapter);
        return view;
    }

    private List<ListItem> getListItem() {
        List<ListItem> mItems = new ArrayList<>();

        String dirPathImage = getContext().getFilesDir().getAbsolutePath() + "/" + Const.IMG_PRFX;
        File dirFileImage = new File(dirPathImage);
        String[] dirList = dirFileImage.list();

        for (String dir : dirList) {
            if (Util.getInvisibleFile(getContext(), dir, Const.IMG_PRFX).exists()) {
                // if setting invisible by setting menu, do not show.
                continue;
            }
            String dirName = Util.getDirName(getContext(), dir, Const.IMG_PRFX);
            GalleryData data = new GalleryData();
            data.importFromFile(getContext(), dirName, dir);
            if (data.picUrls != null && data.picUrls.size() > 0) {
                ListItem item = new ListItem();
                item.filePath = data.picUrls.get(0);
                item.title = dirName;
                mItems.add(item);
            }
        }

        return mItems;
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{
        private List<ListItem> mItems;
        private LayoutInflater inflater;
        private int imageViewWidth;
        private int imageViewHeight;

        public class ViewHolder extends RecyclerView.ViewHolder{
            public CardView cardView;
            public ImageView  imgView;
            public TextView textView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                imgView = itemView.findViewById(R.id.imageView);
                textView = itemView.findViewById(R.id.titleGallery);
            }
        }

        public MyAdapter(Context context, List<ListItem> items) {
            mItems = items;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            WindowManager wm = (WindowManager)getActivity().getSystemService(WINDOW_SERVICE);
            Display disp = wm.getDefaultDisplay();
            Point size = new Point();
            disp.getSize(size);

            int screenWidth = size.x;
            int screenHeight = size.y;

            imageViewWidth = screenWidth / 2;
            imageViewHeight = (int) ((float)imageViewWidth * (2.0/3.0) );
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            // Create a new view.
            View v = inflater.inflate(R.layout.map_entrance_item, viewGroup, false);
            // you can also set view size here. like this
            // ViewGroup.LayoutParams params = v.getLayoutParams();
            // params.height = view_size;
            // v.setLayoutParams(params);
            return new MyAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            ListItem item = mItems.get(i);

//            viewHolder.imgView.setLayoutParams(new ViewGroup.LayoutParams(imageViewWidth,imageViewHeight));
            Bitmap bitmap = Util.createGalleryBitmap(item.filePath, getContext());
            if (bitmap != null) {
                viewHolder.imgView.setImageBitmap(bitmap);
                viewHolder.imgView.setMaxHeight(imageViewHeight);
                viewHolder.imgView.setMaxWidth(imageViewWidth);
            }
            viewHolder.textView.setText(item.title);
            viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private class ListItem {
        public String filePath;
        public String title;
    }


}
