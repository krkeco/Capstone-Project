package com.krkeco.dateit;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ReturnActivity extends AppCompatActivity {

    LinearLayout main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //get bundle/details and create separate textbox for each date with onclick to add to calendar
        main = (LinearLayout) findViewById(R.id.return_llayout);

        TextView introText = new TextView(this);
        introText.setText("You are free to meet:");
        main.addView(introText);
        //for each available time...
        TextView textView = new TextView(this);
        textView.setText("12/15/17 from 3-5pm");
        main.addView(textView);

    }
}
