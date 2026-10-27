package com.adskipper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Ad Skipper");
        title.setTextSize(24);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        Button enable = new Button(this);
        enable.setText("Open accessibility settings");
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        root.addView(enable);

        CheckBox auto = new CheckBox(this);
        auto.setText("Auto-click \"Skip Ad\"");
        auto.setChecked(Prefs.isAutoSkip(this));
        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                Prefs.setAutoSkip(MainActivity.this, isChecked);
            }
        });
        root.addView(auto);

        CheckBox mute = new CheckBox(this);
        mute.setText("Mute media volume while an ad plays");
        mute.setChecked(Prefs.isMute(this));
        mute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                Prefs.setMute(MainActivity.this, isChecked);
            }
        });
        root.addView(mute);

        TextView note = new TextView(this);
        note.setText("Turn on \"Ad Skipper\" in Settings > Accessibility. "
            + "It only acts on the YouTube app.\n\n"
            + "On Android 13+, if the toggle is greyed out: Settings > Apps > Ad Skipper > "
            + "three-dot menu > Allow restricted settings.");
        note.setPadding(0, dp(16), 0, 0);
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceEnabled()) {
            status.setText("Status: ON");
        } else {
            status.setText("Status: OFF (enable it in Accessibility settings)");
        }
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) {
            return false;
        }
        String me = getPackageName() + "/" + SkipService.class.getName();
        return enabled.toLowerCase().contains(me.toLowerCase());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
