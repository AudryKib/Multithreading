package com.mobilecomputing.new_automaton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

public class ConversionActivity extends AppCompatActivity {

    EditText textView_retrieved;
    Button button_conversion;
    Button display;
    TextView edit_text;
    ImageView downloadedImage;

    private static final String TAG = ConversionActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversion);

        Random random = new Random();
        int gen = random.nextInt();
        String scanned_text = getIntent().getStringExtra("scanned_text");
        textView_retrieved = findViewById(R.id.text_view_retrieved);
        button_conversion = findViewById(R.id.button_conversion);
        textView_retrieved.setText(scanned_text);
        edit_text = findViewById(R.id.edit_text);
        display = findViewById(R.id.button_display);
        downloadedImage = findViewById(R.id.image_view_image_downloaded);

        // I am using this to setup aws transfer utility
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        button_conversion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_text.setVisibility(View.VISIBLE);
                downloadedImage.setVisibility(View.GONE);
                String scanned_text =textView_retrieved.getText().toString();
                NFAConverter nfaConverter = new NFAConverter();
                edit_text.setText(nfaConverter.toGraphViz( nfaConverter.compile(scanned_text)));

                File file = new File(getApplicationContext().getFilesDir(),"input"+gen+".gv");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.append(nfaConverter.toGraphViz( nfaConverter.compile(scanned_text)));
                    writer.close();
                }
                catch(Exception e) {
                    Log.e(TAG, e.getMessage());
                }

                // Initialize the AWSMobileClient if not initialized
                AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
                        uploadWithTransferUtility(file, gen);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Initialization error.", e);
                    }
                });

            }
        });

        display.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit_text.setVisibility(View.GONE);
                downloadedImage.setVisibility(View.VISIBLE);

                // Initialize the AWSMobileClient if not initialized
                AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
                        downloadWithTransferUtility();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Initialization error.", e);
                    }
                });


                File downloadedFile = new File(getApplicationContext().getFilesDir(),"output.png");


                if(downloadedFile.exists()){
                    Bitmap myBitmap = BitmapFactory.decodeFile(downloadedFile.getAbsolutePath());
                    downloadedImage.setImageBitmap(myBitmap);

                }else {
                    Toast.makeText(ConversionActivity.this,"Image failed to Display", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }



// putting this on the side for now
//    private void downloadFile(String file){
//
//        Amplify.Storage.downloadFile(
//                file,
//                new File(getApplicationContext().getFilesDir() + "/output.png"),
//                StorageDownloadFileOptions.defaultInstance(),
//                progress -> Log.i("MyAmplifyApp", "Fraction completed: " + progress.getFractionCompleted()),
//                result -> Log.i("MyAmplifyApp", "Successfully downloaded: " + result.getFile().getName()),
//                error -> Log.e("MyAmplifyApp",  "Download Failure", error)
//        );
//
//
//    }

    /**
     * Function to upload a file to aws
     * **/

    public void uploadWithTransferUtility(File f, int gen) {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIARCCE5UO7VZBLYP4T", "XvXWOh43/zgJCvTNEE3SQRsRiITewEgNBNMIJB/i");

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(awsCreds, Region.getRegion("us-east-1")))
                        .build();


        TransferObserver uploadObserver =
                transferUtility.upload(
                        "public/input"+gen+".gv", new File(getApplicationContext().getFilesDir(),f.getName()));

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Toast.makeText(ConversionActivity.this,"File successfully uploaded", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Toast.makeText(ConversionActivity.this,"Error has occured in file upload", Toast.LENGTH_SHORT).show();
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d(TAG, "Bytes Transferred: " + uploadObserver.getBytesTransferred());
        Log.d(TAG, "Bytes Total: " + uploadObserver.getBytesTotal());
    }


    private void downloadWithTransferUtility() {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIARCCE5UO7VZBLYP4T", "XvXWOh43/zgJCvTNEE3SQRsRiITewEgNBNMIJB/i");
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(awsCreds, Region.getRegion("us-east-1")))
                        .build();

        TransferObserver downloadObserver =
                transferUtility.download(
                        "public/output.png",
                        new File(getApplicationContext().getFilesDir(), "output.png"));

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("Your Activity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Toast.makeText(ConversionActivity.this,"Image failed to download", Toast.LENGTH_SHORT).show();
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("Your Activity", "Bytes Transferred: " + downloadObserver.getBytesTransferred());
        Log.d("Your Activity", "Bytes Total: " + downloadObserver.getBytesTotal());
    }


}