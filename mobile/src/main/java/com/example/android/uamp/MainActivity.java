package com.example.chip7.musicstore;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    DatabaseHelper helper = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //Onclick go to new display.
    public void onButtonClick(View v) {
        if(v.getId() == R.id.Blogin) {

            //Converts textview into string.
            EditText a = (EditText)findViewById(R.id.TFuname);
            String str = a.getText().toString();
            EditText b = (EditText)findViewById(R.id.TFpassword);
            String pass = b.getText().toString();

            //Validates whether the password entered is the same as the the password in the DB.
            String password = helper.searchPass(str);
            if(pass.equals(password)){
                Intent i = new Intent(MainActivity.this, display.class);
                i.putExtra("Username", str);
                startActivity(i);
            }
            else
            {
                //Error Message.
                Toast temp = Toast.makeText(MainActivity.this, "Incorrect Username or Password" , Toast.LENGTH_LONG);
                temp.show();
            }

            Intent i = new Intent(MainActivity.this, display.class);
            i.putExtra("Username", str);
            startActivity(i);
        }
        //If user click sign up button, takes user to sign up activity.
        if(v.getId() == R.id.Bsignup) {
            Intent i = new Intent(MainActivity.this, SignUp.class);
            startActivity(i);
        }
    }
}



