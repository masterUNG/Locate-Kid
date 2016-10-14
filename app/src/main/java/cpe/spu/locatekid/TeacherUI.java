package cpe.spu.locatekid;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class TeacherUI extends AppCompatActivity implements View.OnClickListener {

    //ประกาศตัวแปร
    private TextView nameTextView, surnameTextView, phoneTextView;
    private ImageView avatarImageView;
    private String[] loginStrings;
    private String imagePathString, imageNameString;
    private static final String urlPHP = "http://swiftcodingthai.com/golf1/edit_image_teacher.php";
    Button buttonexit;

    //For NFC
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;


    TextView message;
    Button btnWrite;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_ui);

        //Widget
        nameTextView = (TextView) findViewById(R.id.textView8);
        surnameTextView = (TextView) findViewById(R.id.textView9);
        phoneTextView = (TextView) findViewById(R.id.textView10);
        avatarImageView = (ImageView) findViewById(R.id.imageView3);
        buttonexit = (Button) findViewById(R.id.button8);

        //get ค่าจาก intent ที่แล้วมาใช้
        loginStrings = getIntent().getStringArrayExtra("Login"); //นำค่าจากหน้าที่แล้วมาจาก putextra

        //เช็ค image ว่ารูปมีไหม
        if (loginStrings[4].length() != 0) {
            loadImageAvatar(loginStrings[0]); //เช็คความยาวของตัวอักษรแล้วเทียบถ้ามีให้ส่งลง

        }// if

        //นำค่าจาก database มาโชว์ตามจุดที่ต้องการ
        nameTextView.setText("ชื่อ : " + loginStrings[1]);
        surnameTextView.setText("นามสกุล : " + loginStrings[2]);
        phoneTextView.setText("Phone : " + loginStrings[3]);

        //Image Controller
        avatarImageView.setOnClickListener(this);

        buttonexit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goexit = new Intent(TeacherUI.this, MainActivity.class);
                startActivity(goexit);
                finish();
            }
        });


        //NFC
        context = this;


        message = (TextView) findViewById(R.id.edit_message);
        btnWrite = (Button) findViewById(R.id.button);

        btnWrite.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    if(myTag ==null) {
                        Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    } else {
                        write(message.getText().toString(), myTag);
                        Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };





    }   // Main Method


    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String strTagNFC = "";  // สิ่งที่อ่านได้จาก NFC
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            strTagNFC = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        //Get tagNFC finish

        showDetailStudent(strTagNFC);

    }

    private void showDetailStudent(String strTagNFC) {

        SynStudent synStudent = new SynStudent(TeacherUI.this);
        synStudent.execute(strTagNFC);

    }   // showDetail

    private class SynStudent extends AsyncTask<String, Void, String> {

        //Explicit
        private Context context;
        private static final String urlJSON = "http://swiftcodingthai.com/golf1/get_student.php";
        private String ID_ParentString;
        private boolean aBoolean = true;
        private String[] columnStudent = new String[]{
                "ID_Student",
                "Name_Student",
                "Sur_Student",
                "Class_Student",
                "Address_Student",
                "Pic_Student",
                "ID_Parent"};
        private String[] studentStrings;

        public SynStudent(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... strings) {

            ID_ParentString = strings[0];

            try {

                OkHttpClient okHttpClient = new OkHttpClient();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(urlJSON).build();
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string();

            } catch (Exception e) {
                Log.d("14octV1", "e doInBack ==> " + e.toString());
                return null;
            }

        }   // doInBack

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("14octV1", "JSON ==> " + s);
            Log.d("14octV1", "ID_parent ==> " + ID_ParentString);

            try {

                JSONArray jsonArray = new JSONArray(s);

                studentStrings = new String[columnStudent.length];

                for (int i=0;i<jsonArray.length();i++) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    if (ID_ParentString.equals(jsonObject.getString(columnStudent[6]))) {

                        Log.d("14octV2", "ID_ParntString OK");
                        aBoolean = false;
                        for (int i1=0;i1<columnStudent.length;i1++) {

                            studentStrings[i1] = jsonObject.getString(columnStudent[i1]);
                            Log.d("14octV2", "studentString(" + i1 + ") = " + studentStrings[i1]);

                        }   // for

                    }   // if

                }   // for

                if (aBoolean) {
                    //Search False
                    Alert alert = new Alert();
                    alert.myDialog(context,
                            "หา Tag นี้ไม่พบ",
                            "ไม่มี " + ID_ParentString + " ใน ฐานข้อมูลของเรา");

                } else {
                    Log.d("14octV1", "Tag " + ID_ParentString + " OK");
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }   // onPost

    }   // SynStudent Class


    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }



    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }   // WriteModeOff





    private void loadImageAvatar(String id) {

        Log.d("15SepV3", "Load image at id ==> " + id);
        LoadImage loadImage = new LoadImage(this, id);
        loadImage.execute();

    }//Loadimage

    private class LoadImage extends AsyncTask<Void, Void, String>
    {
        private Context context;
        private String idString;
        private static final String urlPHPimage = "http://swiftcodingthai.com/golf1/get_image_teacher_where_id.php";
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
                        .add("ID_Teacher", idString)
                        .build();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(urlPHPimage).post(requestBody).build();
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string();

            } catch (Exception e) {
                Log.d("15SepV3", "e doIn ==>" + e.toString());
                return null;
            }
        }//doInBack

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("15SepV3", "JSON ==> " + s);

            try {

                JSONArray jsonArray = new JSONArray(s);
                JSONObject jsonObject = jsonArray.getJSONObject(0); //มาจากค่าที่รับมาคือ loginstring0
                String strURLimage = jsonObject.getString("Pic_Teacher"); //ดึงค่าที่ต้องการมาโชว์
                Picasso.with(context).load(strURLimage).resize(120, 150).into(avatarImageView);//ไม่ว่ารูปจะขนาดเท่าไหร่จัดให้เป็นขนาดนี้เลย

            } catch (Exception e) {
                e.printStackTrace();
            }

        }//onPost
    }// method การโหลดรูปภาพจาก database มาแสดง

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.imageView3:
                confirmEditImage();
                break;

        }   // switch เมื่อคลิกที่รูปจะเกิดกิจกรรมนี้ขึ้น

    }   // onClick

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


    }   // confirmEditImage สร้างหน้าต่างการเลือกว่าจะตกลง หรือ ยกเลิก

    private void changeAvatar() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "โปรดเลือกภาพที่ต้องการ"), 1);


    }   // changeAvatar


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

        }   // if

    }   // onActivityresult เช็คค่าจากการ positive ของ edit avatar

    private void uploadImageToServer() {

        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy
                .Builder().permitAll().build();
        StrictMode.setThreadPolicy(threadPolicy); //ช่วยทำให้การอัพโหลดรูปได้

        try {

            SimpleFTP simpleFTP = new SimpleFTP();
            simpleFTP.connect("ftp.swiftcodingthai.com",
                  21, "golf1@swiftcodingthai.com", "Abc12345");
            simpleFTP.bin(); //แปลงเป็น binary โยนไปดัง database
            simpleFTP.cwd("Image"); //กำหนด directory ที่เก็บรูปไว้
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

        EditPicTeacher editPicTeacher = new EditPicTeacher(this);
        editPicTeacher.execute();

    } // uploadImageOnMySQL

    private class EditPicTeacher extends AsyncTask<Void, Void, String> {

        //ประกาศตัวแปร
        private Context context;

        public EditPicTeacher(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            //เป็นส่วนการรับรูปจากการเลือกแล้วนำลงไปยัง ดาต้าเบสเพื่อแสดงผล  (รูปที่ add ล่าสุดจะถูกบันทึกใน database)
            try {

                OkHttpClient okHttpClient = new OkHttpClient();
                RequestBody requestBody = new FormEncodingBuilder()
                        .add("isAdd", "true")
                        .add("ID_Teacher", loginStrings[0])
                        .add("Pic_Teacher", "http://swiftcodingthai.com/golf1/Image/" + imageNameString)
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

    }//editPicTeacher คลาสเปลี่ยนรูป


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
    } // เมธอดการเลือกและนำทางไปหารูปที่ gallery
}   // Main Class