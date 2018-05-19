package com.example.chip7.musicstore;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by chip7 on 14/05/2017.
 */

public class SignUp extends Activity {

    DatabaseHelper helper = new DatabaseHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
    }

    //Allows the user to edit the text in the view.
    public void onSignUpClick(View v) {
        if(v.getId() == R.id.bsignupbutton) {
            EditText name = (EditText)findViewById(R.id.TFname);
            EditText email = (EditText)findViewById(R.id.TFemail);
            EditText uname = (EditText)findViewById(R.id.TFuname);
            EditText pass1 = (EditText)findViewById(R.id.TFpass1);
            EditText pass2 = (EditText)findViewById(R.id.TFpass2);

            //Convert all values to a string value.
            String namestr = name.getText().toString();
            String emailstr = email.getText().toString();
            String unamestr = uname.getText().toString();
            String pass1str = pass1.getText().toString();
            String pass2str = pass2.getText().toString();

            //Validate the passwords match.
            if(!pass1str.equals(pass2str)){
                //Display error message.
                Toast pass = Toast.makeText(SignUp.this, "Password don't match" , Toast.LENGTH_SHORT);
                pass.show();
            }
            else{
                //Insert data into DB.
                Contact c = new Contact();
                c.setName(namestr);
                c.setEmail(emailstr);
                c.setUname(unamestr);
                c.setPass(pass1str);

                helper.insertContact(c);
            }
        }
    }
}
