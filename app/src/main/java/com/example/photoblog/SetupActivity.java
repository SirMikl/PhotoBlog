package com.example.photoblog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI = null;
    private String user_id;
    private boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgres;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = (Toolbar)findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Account Setup");

        firebaseAuth = FirebaseAuth.getInstance();
        user_id = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        setupImage = findViewById(R.id.setup_image);
        setupName = findViewById(R.id.setup_name);
        setupBtn = findViewById(R.id.setup_btn);
        setupProgres = findViewById(R.id.setup_progress);

        setupProgres.setVisibility(View.VISIBLE);
        setupBtn.setEnabled(false);

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint("CheckResult")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if (Objects.requireNonNull(task.getResult()).exists()){
                        //Toast.makeText(SetupActivity.this, "Data exists", Toast.LENGTH_SHORT).show();
                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");
                        mainImageURI = Uri.parse(image);
                        setupName.setText(name);
                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.profile);
                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);
                    } else {
                        //Toast.makeText(SetupActivity.this, "Data doesn't exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String error = Objects.requireNonNull(task.getException()).getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore retrieve Error " + error, Toast.LENGTH_SHORT).show();
                }
                setupProgres.setVisibility(View.INVISIBLE);
                setupBtn.setEnabled(true);
            }
        });

        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String user_name = setupName.getText().toString();
                if(!TextUtils.isEmpty(user_name) && mainImageURI!=null){
                setupProgres.setVisibility(View.VISIBLE);
                if (isChanged){
                    user_id = firebaseAuth.getCurrentUser().getUid();
                    final StorageReference image_path = storageReference.child("profile_images").child(user_id+".jpeg");
                    image_path.putFile(mainImageURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            image_path.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //storeFirestore(taskSnapshot, user_name);
                                    Uri download_uri = uri;
                                    Map<String, String> userMap = new HashMap<>();
                                    userMap.put("name", user_name);
                                    userMap.put("image", download_uri.toString());
                                    firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                Toast.makeText(SetupActivity.this, "User's settings are updated", Toast.LENGTH_SHORT).show();
                                                Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                                                startActivity(mainIntent);
                                                finish();
                                            } else {
                                                String error = Objects.requireNonNull(task.getException()).getMessage();
                                                Toast.makeText(SetupActivity.this, "FireStore Error " + error, Toast.LENGTH_SHORT).show();
                                            }
                                            setupProgres.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                }
                            });
                        }
                    });} else {
                    storeFirestore(null, user_name);
                }}}});
                    /*image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()){
                                storeFirestore(task, user_name);
                            } else {
                                String error = Objects.requireNonNull(task.getException()).getMessage();
                                Toast.makeText(SetupActivity.this, "Image Error " + error, Toast.LENGTH_SHORT).show();
                                setupProgres.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
            } else {
                    storeFirestore(null, user_name);
                }
            }
        });*/

        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                    if (ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        Toast.makeText(SetupActivity.this, "You have already Permission", Toast.LENGTH_SHORT).show();
                        BringImageCropper();
                    }
                } else {
                    BringImageCropper();
                }
            }
        });
    }

    private void storeFirestore(@NonNull UploadTask.TaskSnapshot task, String user_name) {
        Uri download_uri;
       if (task != null) {
            download_uri = task.getUploadSessionUri();
        } else{
            download_uri = mainImageURI;
        }
        //Toast.makeText(SetupActivity.this, "The image is uploaded", Toast.LENGTH_SHORT).show();
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", user_name);
        userMap.put("image", download_uri.toString());
        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(SetupActivity.this, "User's settings are updated", Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                } else {
                    String error = Objects.requireNonNull(task.getException()).getMessage();
                    Toast.makeText(SetupActivity.this, "FireStore Error " + error, Toast.LENGTH_SHORT).show();
                }
                setupProgres.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void BringImageCropper() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert result != null;
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged = true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                assert result != null;
                Exception error = result.getError();
            }
        }
    }
}