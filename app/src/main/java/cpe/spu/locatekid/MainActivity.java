package cpe.spu.locatekid;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    //ประกาศตัวแปร
    private EditText usernameEditText , passwordEditText;
    private String usernameString , passwordString;
    private RadioGroup radioGroup;
    private RadioButton parentRadioButton, teacherRadioButton;
    private int modeChoice = 0;
    private String[] urlPHPStrings = new String[]{"http://swiftcodingthai.com/golf1/get_userparent.php"
            ,"http://swiftcodingthai.com/golf1/get_userteacher.php"};
    private String[] loginStrings;
    private String[] teacherStrings = new String[] {"ID_Teacher", "Name_Teacher", "Sur_Teacher", "Tel_Teacher",
            "Pic_Teacher", "Username", "Password"};
    private String[] parentStrings = new String[] {"ID_Parent", "Name_Parent", "Sur_Parent", "Tel_Parent",
            "Pic_Parent", "Username", "Password"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //widget
        usernameEditText = (EditText) findViewById(R.id.editText);
        passwordEditText = (EditText) findViewById(R.id.editText2);
        radioGroup = (RadioGroup) findViewById(R.id.choiceMode);
        parentRadioButton = (RadioButton) findViewById(R.id.radioButton);
        teacherRadioButton = (RadioButton) findViewById(R.id.radioButton2);

        //กำหนดเงื่อนไขต่าง ๆ ของการเลือกผู้ปกครองและครู
        choiceMode();

    } //main method
    //class ที่ทำหน้าที่เชื่อมต่อกันกับ json
    private class SyncAuthen extends AsyncTask<Void, Void, String> {

        //ประกาศตัวแปร
        private Context context;
        private String getURLString , myUserString, myPasswordString , truePasswordString;
        private Boolean statusABoolean = true;
        private int myModechoiceAnInt = 0;

        public SyncAuthen(int myModechoiceAnInt, Context context, String getURLString, String myUserString, String myPasswordString) {
            this.myModechoiceAnInt = myModechoiceAnInt;
            this.context = context;
            this.getURLString = getURLString;
            this.myUserString = myUserString;
            this.myPasswordString = myPasswordString;
        }

        @Override
        protected String doInBackground(Void... voids) {
            //เช็คค่าการเชื่อมต่อ json กับ database
            try {

                OkHttpClient okHttpClient = new OkHttpClient();
                Request.Builder builder = new Request.Builder();
                Request request = builder.url(getURLString).build();
                Response response = okHttpClient.newCall(request).execute();
                return response.body().string();

            } catch (Exception e) {
                Log.d("11JulV1", "e doInBack ==> " + e.toString());
                return null;
            }
        } //doInBack

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("11JulV1", "JSON ==> " + s);

            try {

                JSONArray jsonArray = new JSONArray(s);
                loginStrings = new String[7]; //จอง memmory ของสตริง

                for (int i=0;i<jsonArray.length();i+=1) {

                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    if (myUserString.equals(jsonObject.getString("Username"))) {

                        statusABoolean = false;
                        truePasswordString = jsonObject.getString("Password");
                        //เช็คละ get data จาก database
                        switch (myModechoiceAnInt){

                            case 0:  //เป็นผู้ปกครอง

                                loginStrings[0] = jsonObject.getString(parentStrings[0]);
                                loginStrings[1] = jsonObject.getString(parentStrings[1]);
                                loginStrings[2] = jsonObject.getString(parentStrings[2]);
                                loginStrings[3] = jsonObject.getString(parentStrings[3]);
                                loginStrings[4] = jsonObject.getString(parentStrings[4]);
                                loginStrings[5] = jsonObject.getString(parentStrings[5]);
                                loginStrings[6] = jsonObject.getString(parentStrings[6]);

                                break;

                            case 1:  //เป็นครูประจำรถ

                                loginStrings[0] = jsonObject.getString(teacherStrings[0]);
                                loginStrings[1] = jsonObject.getString(teacherStrings[1]);
                                loginStrings[2] = jsonObject.getString(teacherStrings[2]);
                                loginStrings[3] = jsonObject.getString(teacherStrings[3]);
                                loginStrings[4] = jsonObject.getString(teacherStrings[4]);
                                loginStrings[5] = jsonObject.getString(teacherStrings[5]);
                                loginStrings[6] = jsonObject.getString(teacherStrings[6]);

                                break;

                        }

                    }//if
                }//for

            //check User
                if (statusABoolean) {
                    // ผู้ใช้ใส่ไม่ตรงกับที่มีใน database
                    Alert alert = new Alert();
                    alert.myDialog(context, "ไม่มี User นี้ในระบบ", "ไม่มี"  +  myUserString  +  "ในระบบ หรือ เลือกผิดโหมด");
                }
                else if (myPasswordString.equals(truePasswordString)) {
                    //Password True
                    Toast.makeText(context, "Welcome", Toast.LENGTH_SHORT).show();

                //รับค่าจาก switch case ข้างบนโยนไปหน้าต่อไปที่ต้องการ
                    switch (myModechoiceAnInt) {

                        case 0: //ผู้ปกครอง

                            Intent intent = new Intent(MainActivity.this, ParentUI.class);
                            intent.putExtra("Login", loginStrings);
                            startActivity(intent);
                            finish();

                            break;

                        case 1: //ครูประจำรถ

                            Intent intent1 = new Intent(MainActivity.this, TeacherUI.class);
                            intent1.putExtra("Login", loginStrings);
                            startActivity(intent1);
                            finish();

                            break;

                    }//switch เช็คคา

                } else {
                    //Password False
                    Alert alert = new Alert();
                    alert.myDialog(context, "Incorrect Password","Please Try Again");
                }

            }catch (Exception e){
                Log.d("11JulV1", "e onPost ==> " + e.toString());
            }

        }
    }//Class Sync


    private void choiceMode() {

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                switch (i) {
                    case R.id.radioButton:
                        modeChoice = 0;
                        break;
                    case R.id.radioButton2:
                        modeChoice = 1;
                        break;
                    default: modeChoice = 0;
                        break;
                }

            }//การเช็คค่าเงื่อนไขการเลือก
        });

    }// Class choicemode

    public void clickButton(View view) {

        //นำค่ามาจาก edittext
        usernameString = usernameEditText.getText().toString().trim();
        passwordString = passwordEditText.getText().toString().trim();

        //การ check ช่องว่าง
        if (usernameString.equals("") || passwordString.equals("")) {
            //มีช่องว่าง
            Alert alert = new Alert();
            alert.myDialog(this, "Error" , "โปรดกรอกให้ครบถ้วน");
        } else {
            //มีการประมวลผล
            SyncAuthen syncAuthen = new SyncAuthen(modeChoice,this, urlPHPStrings[modeChoice]
            ,usernameString , passwordString);
            syncAuthen.execute();
        } //เงื่อนไขเช็คว่า

    } //เมธอดปุ่มล็อคอิน
} //Class main
