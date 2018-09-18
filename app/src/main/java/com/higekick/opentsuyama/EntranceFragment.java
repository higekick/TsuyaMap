package com.higekick.opentsuyama;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EntranceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EntranceFragment extends Fragment {
    public String TAG = EntranceFragment.class.getSimpleName();

    private OnOpenDrawerListener mOpenDrawerListener;

    public interface OnOpenDrawerListener{
        void onExecuteGalleryOpen();
        void onExecuteMapOpen();
    }

    public EntranceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EntranceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EntranceFragment newInstance() {
        EntranceFragment fragment = new EntranceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void setOpenListener(OnOpenDrawerListener l){
        mOpenDrawerListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_entrance, container, false);
        return v;
    }

}