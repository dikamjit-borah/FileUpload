package com.hobarb.fileupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int PICKFILE_RESULT_CODE = 101;
    private static final String TAG = "MainActivity";
    private static final int GALLERY_PERMISSION_CODE = 111;
    Button bChoose;
    Button bUpload;
    TextView tvFileName;
    String filePath;
    File file;
    FirebaseStorage storage;
    StorageReference storageRef;
    StorageReference csvRefs;
    private UploadTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setViews();
        initFirebase();
    }

    private void initFirebase() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void setViews() {
        bChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(isPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)))
                    getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, GALLERY_PERMISSION_CODE);
                else
                    openExplorer();

            }
        });

        bUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (file == null)
                    Toast.makeText(getApplicationContext(), "No file to upload", Toast.LENGTH_SHORT).show();
                else {
                    uploadFile();
                }
            }
        });
    }

    private void uploadFile() {
        Uri file = Uri.fromFile(new File(filePath));
        csvRefs = storageRef.child("csvs/"+file.getLastPathSegment());
        uploadTask = csvRefs.putFile(file);

// Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "onFailure: "+exception.getMessage() );
                Toast.makeText(getApplicationContext(), "File could not be uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(getApplicationContext(), "File successfully uploaded", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void openExplorer() {
        Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            Toast.makeText(getApplicationContext(), "Result not OK", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Uri uri = data.getData();

            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                filePath = getPath(uri);
            }
            else{
                filePath = new FileUtils(getApplicationContext()).getPath(uri);
            }
            tvFileName.setText("" + filePath);
            createFile();
        }
    }

    public String getImagePath(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    private void createFile() {
        String path = Environment.DIRECTORY_PICTURES.toString();
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Whatever/");
        String filename = "loka";
        try {
            if (!folder.exists()) {
                folder.mkdirs();
                System.out.println("Making dirs");
            }
            file= new File(folder.getAbsolutePath(), filename);
            file.createNewFile();

           /* FileOutputStream out = new FileOutputStream(myFile);
            myBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();*/

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPath(Uri uri) {

        String path = null;
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null) {
            path = uri.getPath();
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }

    private void initViews() {
        bChoose = findViewById(R.id.bChoose);
        tvFileName = findViewById(R.id.tvFileName);
        bUpload = findViewById(R.id.bUpload);
    }

    public boolean isPermission(String permissionName) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permissionName) == PackageManager.PERMISSION_GRANTED;
    }

    public void getPermission(String permissionName, int permissionCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{permissionName}, permissionCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GALLERY_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openExplorer();
            } else {
                Toast.makeText(getApplicationContext(), "Permission to Gallery was denied", Toast.LENGTH_SHORT);
            }
        }

    }
}