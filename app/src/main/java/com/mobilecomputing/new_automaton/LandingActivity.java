package com.mobilecomputing.new_automaton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LandingActivity extends AppCompatActivity {

     Button Start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Start = findViewById(R.id.button_get_started);

        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getStartedActivityIntent = new Intent(LandingActivity.this, MainActivity.class);
                startActivity(getStartedActivityIntent);
            }
        });


    }
}