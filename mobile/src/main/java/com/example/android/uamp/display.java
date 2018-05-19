package com.example.chip7.musicstore;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by chip7 on 14/05/2017.
 */

public class display extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);

        String username = getIntent().getStringExtra("Username");

        TextView tv = (TextView)findViewById(R.id.TVusername);
        tv.setText(username);

    }
}
