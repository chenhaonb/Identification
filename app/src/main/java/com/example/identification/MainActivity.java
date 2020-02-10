package com.example.identification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int CHOOSE_PHOTO= 2;
    private static final int TAKE_CAMERA=1;
    private ProgressDialog progressDialog;
    private Uri imageUri;
    private ImageView picture;
    private Button buttonPhoto;
    private Button buttonCamera;
    private Button buttonWatch;
    private Bitmap bitmap = null;
    private String imagePath;
    private String reslut;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        picture = (ImageView)findViewById(R.id.picture);
        buttonCamera = (Button) findViewById(R.id.button_camera);
        buttonPhoto = (Button)findViewById(R.id.button_photo);
        buttonWatch = (Button)findViewById(R.id.button_watch);
        buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
            }
        });
        buttonWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Thread t = new Thread(new Runnable() {
                   @Override
                   public void run() {
                       try{
                           OkHttpClient client = new OkHttpClient();
                           File file = new File(imagePath);
                           MultipartBody.Builder builder = new MultipartBody.Builder()
                                   .setType(MultipartBody.FORM)
                                   .addFormDataPart("name","imageName");
                           builder.addFormDataPart("headimg",file.getName(),RequestBody.create(MediaType.parse("image/*"), file));
                           RequestBody requestBody =builder.build();
                           Request request = new Request.Builder()
                                   .url("http://121.40.174.157:8000/users/views/")
                                   .post(requestBody)
                                   .build();
                           Response response = client.newCall(request).execute();
                           reslut= response.body().string();
                           Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                           intent.putExtra("result", reslut);
                           startActivity(intent);
                       }catch (Exception e){
                          e.printStackTrace();
                       }
                   }
               });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, 1000);
                } else {
                    File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                    try {
                        if(outputImage.exists()){
                            outputImage.delete();
                        }
                        outputImage.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(Build.VERSION.SDK_INT >= 24){
                        //File对象转换为Uri标识对象
                        imageUri = FileProvider.getUriForFile(MainActivity.this,
                                "com.example.identification.fileprovider", outputImage);
                    }else{
                        //指定图片的输出地址
                        imageUri = Uri.fromFile(outputImage);
                    }
                    //隐式Intent，启动相机程序
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                    startActivityForResult(intent,TAKE_CAMERA);
                }
            }
        });

    }
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(MainActivity.this, "你已经拒绝了该权限", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1000:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, "相机权限已申请", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "相机权限已被禁止,请在设置中打开", Toast.LENGTH_SHORT).show();

                    }
                break;
            default:
        }
    }
    private void handleImageOnKitKat(Intent data) {
        //获取资源定位符
        Uri uri = data.getData();
        //选择图片路径
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(MainActivity.this, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    imagePath = getImagePath(contentUri, null);
                }

            } else if ("content".equalsIgnoreCase(uri.getScheme())) {

                imagePath = getImagePath(uri, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                imagePath = uri.getPath();
            }
        }
        showImage(imagePath);
    }

    private String getImagePath(Uri externalContentUri, String selection) {
        Cursor cursor = getContentResolver().query(externalContentUri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return imagePath;
    }
    private  void handleImageBeforeKitKat(Intent data){
        Uri uri = data.getData();
        String imagePath = getImagePath(uri,null);
        showImage(imagePath);
    }
    private void showImage(String imagePath) {
        if (imagePath != null) {
            bitmap = BitmapFactory.decodeFile(imagePath);
            picture.setImageBitmap(bitmap);
        } else {
            Toast.makeText(MainActivity.this, "没有找到对应图片", Toast.LENGTH_SHORT).show();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case CHOOSE_PHOTO:
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageOnKitKat(data);
                    }
                    break;
                case TAKE_CAMERA:
                    try {
                        bitmap = BitmapFactory.decodeStream(getContentResolver().
                                openInputStream(imageUri));
                       picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }







}
