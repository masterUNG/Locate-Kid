package cpe.spu.locatekid;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import org.jibble.simpleftp.SimpleFTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class ParentUI extends AppCompatActivity implements View.OnClickListener {

    //ประกาศตัวแปร
    private TextView nameTextview, surnameTextview, phoneTextview;
    private ImageView avatarImageView;
    private String[] loginStrings;
    private String imagePathString, imageNameString;
    private static final String urlPHP = "http://swiftcodingthai.com/golf1/edit_image_parent.php";
    Button buttonexit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_ui);

        //widget
        nameTextview = (TextView) findViewById(R.id.textView4);
        surnameTextview = (TextView) findViewById(R.id.textView5);
        phoneTextview = (TextView) findViewById(R.id.textView6);
        avatarImageView = (ImageView) findViewById(R.id.imageView2);
        buttonexit = (Button) findViewById(R.id.button2);

        //get ค่าจาก intent ที่แล้วมาใช้
        loginStrings = getIntent().getStringArrayExtra("Login"); //นำค่าจากหน้าที่แล้วมาจาก putextra

        //เช็ค image ว่ารูปมีไหม
        if (loginStrings[4].length() != 0) {
            loadImageAvatar (loginStrings[0]);
        }//if


        //นำค่าจาก database มาโชว์ตามจุดที่ต้องการ
        nameTextview.setText("ชื่อ : "+ loginStrings[1]);
        surnameTextview.setText("นามสกุล : "+ loginStrings[2]);
        phoneTextview.setText("Phone : "+ loginStrings[3]);

        //Image controller
        avatarImageView.setOnClickListener(this);


        buttonexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goexit = new Intent(ParentUI.this, MainActivity.class);
                startActivity(goexit);
                finish();
            }
        });

    }//main method

    private void loadImageAvatar(String id) {

        Log.d("15SepV3", "Load image at id ==> " + id);
        LoadImage loadImage = new LoadImage(this, id);
        loadImage.execute();

    }//Loadimage


    private class LoadImage extends AsyncTask<Void, Void, String>
    {
        private Context context;
        private  String idString;
        private static final String urlPHPimage = "http://swiftcodingthai.com/golf1/get_image_parent_where_id.php";
        public LoadImage(Context context, String idString) {
            this.context = context;
            this.idString = idString;
        }


        @Override
        protected String doInBackground(Void... voids) {
            //เช็คค่าว่าที่ได้จากการโหลดรูปจาก database
            try {
                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = new FormEncodingBuilder()
                        .add("isAdd", "true")
                        .add("ID_Parent", idString)
                        .build();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(urlPHPimage).post(requestBody).build();
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string();
            } catch (Exception e) {
                Log.d("15SepV3", "e doIn ==>" + e.toString());
                return null;
            }
        }//doInback

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("15SepV3", "JSON ==> " + s);

            try {

                JSONArray jsonArray = new JSONArray(s);
                JSONObject jsonObject = jsonArray.getJSONObject(0); //มาจากค่าที่รับมาคือ loginstring 0
                String strURLimage = jsonObject.getString("Pic_Parent"); //ดึงค่าที่ต้องการมาโชว์
                Picasso.with(context).load(strURLimage).resize(120, 150).into(avatarImageView); //ไม่ว่ารูปจะขนาดเท่าไหร่จัดให้เป็นขนาดนี้เลย
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//OnPost
    }// method การโหลดรูปภาพจากเครื่องลง database

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.imageView2 :
                confirmEditImage();
                break;

        }// switch เมื่อคลิกที่รูปจะเกิดกิจกรรมนี้ขึ้น

    }// onClick

    private void confirmEditImage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Change Image?");
        builder.setIcon(R.drawable.rat48);
        builder.setMessage("คุณต้องการเปลี่ยนรูปหรือไม่ ?");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                changeAvatar();
                dialogInterface.dismiss();
            }
        });
        builder.show();

    } // confirmEditImage สร้างหน้าต่างการเลือกว่าจะตกลง หรือ ยกเลิก

    private void changeAvatar() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent.createChooser(intent, "โปรดเลือกภาพที่ต้องการ"), 1);

    } //changeAvatar

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == 1) && (resultCode == RESULT_OK)) {

            Log.d("15SepV1", "Choose Image OK");

            //get path image
            Uri uri = data.getData();
            imagePathString = myFindPathOfImage(uri);

            Log.d("15SepV1", "imagePathString ==> " + imagePathString);

            //Get ชื่อรูปภาพที่ได้มา
            imageNameString = imagePathString.substring(imagePathString.lastIndexOf("/") + 1);

            Log.d("15SepV1", "imageNameString ==> " + imageNameString);

            uploadImageToServer();

        } //if
    }// onActivityresult เช็คค่าจากการ positive ของ edit avatar

    private void uploadImageToServer() {

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(threadPolicy); // ช่วยทำให้การอัพโหลดรูปได้

        try {

            SimpleFTP simpleFTP = new SimpleFTP();
            simpleFTP.connect("ftp.swiftcodingthai.com",
                    21, "golf1@swiftcodingthai.com", "Abc12345");
            simpleFTP.bin(); //แปลงเป็น binary โยนไปดัง database
            simpleFTP.cwd("Picparent"); //กำหนด directory ที่เก็บรูปไว้
            simpleFTP.stor(new File(imagePathString));
            simpleFTP.disconnect();

            updateImageOnMySQL();

            Toast.makeText(this, "Upload " + imagePathString + "Finished",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d("15SepV1", "e ==> " + e.toString());
        }
    }// method อัพรูปขึ้น server

    private void updateImageOnMySQL() {

        EditPicParent editPicParent = new EditPicParent(this);
        editPicParent.execute();
    }

    private class EditPicParent extends AsyncTask<Void, Void, String> {

        //ประกาศตัวแปร
        private Context context;

        public EditPicParent(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            //เป็นส่วนการรับรูปจากการเลือกแล้วนำลงไปยัง ดาต้าเบสเพื่อแสดงผล  (รูปที่ add ล่าสุดจะถูกบันทึกใน database)
            try {

                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = new FormEncodingBuilder()
                        .add("isAdd", "true")
                        .add("ID_Parent", loginStrings[0])
                        .add("Pic_Parent", "http://swiftcodingthai.com/golf1/Picparent/" + imageNameString)
                        .build();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(urlPHP).post(requestBody).build();
                Response response = okHttpClient.newCall(request).execute();

                return response.body().string();

            } catch (Exception e) {
                Log.d("15SepV2", "e doIn ==> " + e.toString());
                return null;
            }
        }//doInBack

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("15SepV2", "Result ==> " + s);
            loadImageAvatar(loginStrings[0]);

        }//onPost
    }//editPicTeacher เมธอดการเปลี่ยนรูป


    private String myFindPathOfImage(Uri uri) {

        String strResult = null;

        String[] columnStrings = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, columnStrings,
                null, null, null);

        if (cursor != null) {

            cursor.moveToFirst();
            int intColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA); //เข้าไปที่พาร์ทที่มีไฟล์ภาพใน gallery เพื่อนำมาใช้ในการอัพโหลด
            strResult = cursor.getString(intColumnIndex);
        } else {
            strResult = uri.getPath();
        }
        return strResult;

    }// เมธอดการเลือกและนำทางไปหารูปที่ gallery


}// main class
