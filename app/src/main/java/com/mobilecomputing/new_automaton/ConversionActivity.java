package com.mobilecomputing.new_automaton;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;

import java.io.File;

public class ConversionActivity extends AppCompatActivity {

    TextView textView_retrieved;
    Button button_conversion;
    TextView edit_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversion);

        String scanned_text = getIntent().getStringExtra("scanned_text");
        textView_retrieved = findViewById(R.id.text_view_retrieved);
        button_conversion = findViewById(R.id.button_conversion);
     //   textView_retrieved.setText(scanned_text);
        textView_retrieved.setText("a*b");
        edit_text = findViewById(R.id.edit_text);

        button_conversion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String scanned_text =textView_retrieved.getText().toString();
                NFAConverter nfaConverter = new NFAConverter();
                edit_text.setText(nfaConverter.toGraphViz( nfaConverter.compile(scanned_text)));
               // nfaConverter.writeGraphViz("output.gv", nfaConverter.compile(scanned_text));
            }
        });
    }


    private void downloadFile(String file){

        Amplify.Storage.downloadFile(
                file,
                new File(getApplicationContext().getFilesDir() + "/output.png"),
                StorageDownloadFileOptions.defaultInstance(),
                progress -> Log.i("MyAmplifyApp", "Fraction completed: " + progress.getFractionCompleted()),
                result -> Log.i("MyAmplifyApp", "Successfully downloaded: " + result.getFile().getName()),
                error -> Log.e("MyAmplifyApp",  "Download Failure", error)
        );


    }

}