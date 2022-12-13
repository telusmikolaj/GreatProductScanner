package pl.waw.great.greatproductscanner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.chromium.net.CronetEngine;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private MaterialButton cameraBtn;
    private MaterialButton scanBtn;
    private MaterialButton loadBtn;
    private MaterialButton sendBarcodeBtn;
    private ImageView imageView;
    private TextView resultTv;
    private String scannedBarcode;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    private String[] cameraPermissions;
    private String[] storagePermissions;

    private Uri imageUri = null;

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;
    private static final String TAG = "MAIN_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = findViewById(R.id.scanBtn);
        cameraBtn = findViewById(R.id.cameraBtn);
        loadBtn = findViewById(R.id.loadBtn);
        imageView = findViewById(R.id.imageIv);
        resultTv = findViewById(R.id.resultTv);
        sendBarcodeBtn = findViewById(R.id.sendBarcode);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        barcodeScannerOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        cameraBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (checkCameraPermission()) {
                    resultTv.setText("");
                    pickImageCamera();
                } else {
                    requestCameraPermission();
                }
            }
        });

        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStoragePermission()) {
                    resultTv.setText("");
                    pickGalleryImage();
                } else {
                    requestStoragePermission();
                }
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null) {
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                } else {
                    detectBarcodeFromImage();
                }
            }
        });

        sendBarcodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scannedBarcode == null) {
                    Toast.makeText(MainActivity.this, "Najpierw zeskanuj kod", Toast.LENGTH_SHORT).show();
                } else {
                    sendBarcode();
                }
            }
        });

    }


    private void sendBarcode() {
        ApiInterface apiInterface = RetrofitClinet.getRetroFitInstance().create(ApiInterface.class);
        BarcodeScan barcodeScan = new BarcodeScan(scannedBarcode);
        Call<BarcodeScan> call = apiInterface.sendBarcode(barcodeScan);
        call.enqueue(new Callback<BarcodeScan>() {
            @Override
            public void onResponse(Call<BarcodeScan> call, Response<BarcodeScan> response) {
                Log.e(TAG, "onResponse: " + response.code());
                Toast.makeText(MainActivity.this, "Zeskanowany kod zostal przeslany", Toast.LENGTH_SHORT).show();
                resultTv.setText("");
            }

            @Override
            public void onFailure(Call<BarcodeScan> call, Throwable t) {
                Log.e(TAG,"oneFailure: " + t.getMessage());
                resultTv.setText("");

                Toast.makeText(MainActivity.this, "Zeskanowany kod zostal przeslany", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void detectBarcodeFromImage() {
        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            extractBarcodeInfo(barcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Blad skanowania z powodu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Nie mozna odczytac kodu ze zdjecia", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarcodeInfo(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            scannedBarcode = barcode.getRawValue();
            Log.d(TAG, "extractBarcodeInfo: rawValuie: " + scannedBarcode);
            int valueType = barcode.getValueType();

           resultTv.setText("Odczytany kod: " + scannedBarcode);
        }
    }

    private void pickGalleryImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);

    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        imageView.setImageURI(imageUri);
                    } else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult: image Uri: " + imageUri);
                        imageView.setImageURI(imageUri);
                    } else {
                        Toast.makeText(MainActivity.this, "Cancelled ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return result;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean resultStroage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return resultCamera && resultStroage;
    }

    private void requestCameraPermission () {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && storageAccepted) {
                        pickImageCamera();
                    } else {
                        Toast.makeText(this, "Camera & Storage permission are required, ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(storageAccepted) {
                        pickGalleryImage();
                    } else {
                        Toast.makeText(this, "Storage permission is required, ", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}


