package com.higekick.opentsuyama;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.higekick.opentsuyama.util.ImageAdapter;
import com.higekick.opentsuyama.util.Util;
import com.squareup.picasso.Picasso;

import java.io.File;

import static android.content.Context.WINDOW_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainGalleryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainGalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainGalleryFragment extends Fragment implements IMainFragmentExecuter{
    GridView imageGrid;
    GalleryData galleryData;
    Activity activity;

    private OnFragmentInteractionListener mListener;

    public MainGalleryFragment() {
        // Required empty public constructor
    }

    public void setActivity(Activity a, GalleryData d) {
        activity = a;
        galleryData = d;
    }

    public static MainGalleryFragment newInstance() {
        MainGalleryFragment fragment = new MainGalleryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.action_map).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_map) {
            startGoogleMapFromGallery();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startGoogleMapFromGallery(){
        String title = (String) getActivity().getTitle();
        String encodeQuery = Uri.encode("津山　" + title);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(
                "http://maps.google.com/maps?z=12&q=" + encodeQuery));
        startActivity(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_gallery, container, false);

        imageGrid = (GridView) view.findViewById(R.id.imageGrid);

        imageGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                String url = galleryData.picUrls.get(position);
                String name = galleryData.name;
                ImageDialogFragment frg = ImageDialogFragment.newInstance(url,name + position);
                frg.show(getFragmentManager(),"imageDialog");
            }
        });

        if (mListener != null) {
            mListener.onSetOptionItemVisibility(R.id.action_map, true);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        executeLoading(galleryData);
    }

    public static class ImageDialogFragment extends DialogFragment{

        public static ImageDialogFragment newInstance(String url, String name){
            ImageDialogFragment frg = new ImageDialogFragment();
            Bundle args = new Bundle();
            args.putString("url", url);
            args.putString("name", name);
            frg.setArguments(args);
            return frg;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String url = getArguments().getString("url");
            final String name = getArguments().getString("name");
            final ImageView imageView = new ImageView(getActivity());

            LinearLayout lay = new LinearLayout(getActivity());
            lay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            WindowManager wm = (WindowManager)getActivity().getSystemService(WINDOW_SERVICE);
            Display disp = wm.getDefaultDisplay();

            Point size = new Point();
            disp.getSize(size);

            int screenWidth = size.x;
            int screenHeight = size.y;

            int imageViewWidth = screenWidth / 3;
            int imageViewHeight = (int) ((float)imageViewWidth * (2.0/3.0) );

            imageView.setLayoutParams(new ViewGroup.LayoutParams(screenWidth,screenHeight));
            lay.addView(imageView);
            Bitmap bitmap = Util.createGalleryBitmap(url, getContext());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }

//            Picasso
//                    .get()
//                    .load(url)
//                    .placeholder(R.drawable.ic_photo_grey_50_18dp)
//                    .fit()
//                    .centerInside()
//                    .error(R.drawable.ic_error_outline_red_300_36dp)
//                    .into(imageView);

            imageView.invalidate();

            return new AlertDialog.Builder(getActivity())
                    .setPositiveButton("共有する", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // 画像共有するのはPermissionとかいろいろ必要そう
//                            Intent intent=new Intent();
//                            intent.setAction(Intent.ACTION_SEND);
//                            intent.setType("image/jpeg");
//                            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ url));
//                            startActivity(intent);
                            // Todo パーミッションとかいらないのか？
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            File file = new File(url);
                            Uri uri = FileProvider.getUriForFile(getContext(), "com.higekick.opentsuyama.fileprovider", file);
                            intent.setDataAndType(uri, "image/*");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            //@FIXME to blunk mailto, but id dosent't
                            Uri uriMailTo = Uri.parse("mailto:");
                            intent.putExtra(Intent.ACTION_SENDTO, uriMailTo);
                            intent.putExtra(Intent.EXTRA_EMAIL, "");
                            startActivity(intent);

//                            boolean result = Util.createFolderSaveImage(bm, name, getContext());
//                            if (result) {
//                                Toast.makeText(getContext(),"保存しました。" + name, Toast.LENGTH_LONG).show();
//                            } else {
//                                Toast.makeText(getContext(),"保存に失敗しました。", Toast.LENGTH_LONG).show();
//                            }
                        }
                    })
                    .setNegativeButton("閉じる", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                           dismiss();
                        }
                    })
                    .setView(lay)
                    .create();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mListener!=null) {
            mListener.onSetOptionItemVisibility(R.id.action_map, false);
            mListener = null;
        }
    }

    @Override
    public void executeLoading(AbstractContentData d) {
        galleryData = (GalleryData) d;
        imageGrid.setAdapter(new ImageAdapter(getActivity() == null? activity:getActivity(), galleryData.picUrls));
        activity.setTitle(d.name);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onSetOptionItemVisibility(int idMenu, boolean ifVisible);
    }
}
