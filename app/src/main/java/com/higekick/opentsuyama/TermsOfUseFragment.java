package com.higekick.opentsuyama;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.higekick.opentsuyama.util.Const;
import com.higekick.opentsuyama.util.Util;

import java.io.InputStream;

public class TermsOfUseFragment extends Fragment {

    boolean fromSetting;

    private static final String ARG_PARAM1 = "param1";

    public TermsOfUseFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static TermsOfUseFragment newInstance(boolean fromSetting) {
        TermsOfUseFragment fragment = new TermsOfUseFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM1, fromSetting);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =inflater.inflate(R.layout.fragment_terms_of_use, container, false);
        fromSetting = getArguments().getBoolean(ARG_PARAM1);
        setUp(v);
        return v;
    }

    private void setUp(View v) {
        // load terms of use
        try {
            TextView textView = v.findViewById(R.id.text_terms_of_use);
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.terms_of_use);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            textView.setText(new String(b));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set up buttons and checkbox
        CheckBox chkBox = v.findViewById(R.id.chk_termsOfUse);
        final Button btnGallery = v.findViewById(R.id.btnGallery);
        final Button btnMap = v.findViewById(R.id.btnMap);

        if (fromSetting) {
            chkBox.setVisibility(View.INVISIBLE);
            btnGallery.setVisibility(View.INVISIBLE);
            btnMap.setVisibility(View.INVISIBLE);
        }

        chkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int ifVisible = isChecked == true ? View.VISIBLE : View.INVISIBLE;
                btnGallery.setVisibility(ifVisible);
                btnMap.setVisibility(ifVisible);
            }
        });

        v.findViewById(R.id.btnGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Util.netWorkCheck(getContext())) {
                    return;
                }
                Util.setPreferenceValue(getContext(), Const.KEY_CURRENT_USE, Const.CURRENT_USE_IMAGE);
                startMainActivity();
            }
        });
        v.findViewById(R.id.btnMap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Util.netWorkCheck(getContext())) {
                    return;
                }
                Util.setPreferenceValue(getContext(), Const.KEY_CURRENT_USE, Const.CURRENT_USE_MAP);
                startMainActivity();
            }
        });
    }

    private void startMainActivity(){
        Intent intent = new Intent(getContext(), MainActivity.class);
        getActivity().startActivity(intent);
    }

}
