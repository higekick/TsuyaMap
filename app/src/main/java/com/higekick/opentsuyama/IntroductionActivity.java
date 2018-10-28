package com.higekick.opentsuyama;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.higekick.opentsuyama.util.Const;

public class IntroductionActivity extends AppCompatActivity {
    Button btnNext;
    String tagCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);
        btnNext = findViewById(R.id.btn_intro_next);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action!=null && action.equals(Const.ACTION_TERMS_FROM_SETTINGS)) {
            btnNext.setText(R.string.btn_intro_back);
            TermsOfUseFragment fragment = TermsOfUseFragment.newInstance(true);
            replaceFragment(fragment);
        } else {
            btnNext.setText(R.string.btn_intro_next);
            EntranceFragment fragment = EntranceFragment.newInstance();
            replaceFragment(fragment);
        }
        setup();
    }

    private void setup() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnNext.getText().equals(getResources().getString(R.string.btn_intro_back))) {
                    finish();
                }

                if (tagCurrentFragment.equals(EntranceFragment.class.getSimpleName())) {
                    TermsOfUseFragment fragment = TermsOfUseFragment.newInstance(false);
                    replaceFragment(fragment);
                    btnNext.setVisibility(View.GONE);
                }

            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName()).commit();
        tagCurrentFragment = fragment.getClass().getSimpleName();
    }
}
