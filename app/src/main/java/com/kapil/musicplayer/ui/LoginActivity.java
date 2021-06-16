package com.kapil.musicplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import com.kapil.musicplayer.R;

import java.util.HashMap;
import java.util.Map;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText emailText,passwordText,emailSignup,passwordSignup;
    private Button loginButton,signupButton;
    private TextView signUp,loginText;
    private ProgressBar progressBar;
    private LinearLayout loginScreen,signupScreen;
    private String url = "https://music-streaming-service.herokuapp.com/";
    private String username;

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = findViewById(R.id.progress_circular);
        emailText = findViewById(R.id.input_email);
        emailSignup = findViewById(R.id.email);

        passwordText = findViewById(R.id.input_password);
        passwordSignup = findViewById(R.id.password);

        loginButton = findViewById(R.id.btn_login);
        signupButton = findViewById(R.id.btn_signup);

        signUp = findViewById(R.id.link_signup);
        loginText = findViewById(R.id.link_login);

        loginScreen = findViewById(R.id.login_screen);
        signupScreen = findViewById(R.id.signup_screen);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(emailText.getWindowToken(),0);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
                showProgressBar();
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
                showProgressBar();
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginScreen.setVisibility(View.GONE);
                signupScreen.setVisibility(View.VISIBLE);
            }
        });

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginScreen.setVisibility(View.VISIBLE);
                signupScreen.setVisibility(View.GONE);
            }
        });
        automaticLogin();
    }

    private void automaticLogin() {
        String loginUrl = url + "user/login_with_token";
//        showProgressBar();
        volleyGet(loginUrl, null,"Automatic Login");
        showProgressBar();
    }

    private void openMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    private void signUp () {
        Log.d(TAG, "signUp: ");


        String email = emailSignup.getText().toString();
        String password = passwordSignup.getText().toString();

        Log.d(TAG, email);

        if ((email != null && !email.isEmpty()) &&
                (password != null && !password.isEmpty())) {
            String signupUrl = url + "user/signup";
            Log.d(TAG, signupUrl);
            username = email;
            JSONObject postData = new JSONObject();
            try {
                postData.put("username", email);
                postData.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
                hideProgressBar();
            }
            volleyPost(signupUrl, postData, "Signup");
        } else {
            Toast.makeText(this,"Wrong data",Toast.LENGTH_SHORT).show();
        }
    }

    private void login () {
        Log.d(TAG, "login: ");

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        Log.d(TAG, "login: " + email);
        Log.d(TAG, "login: " + password);

        if ((!email.isEmpty()) &&
                ( !password.isEmpty())) {
            username = email;
            String loginUrl = url + "user/login";
            JSONObject postData = new JSONObject();
            try {
                postData.put("email", email);
                postData.put("password", password);
            } catch (JSONException e) {
                e.printStackTrace();
                hideProgressBar();
            }
            volleyGet(loginUrl, postData, "Login");
        } else {
            Toast.makeText(this,"Wrong data",Toast.LENGTH_SHORT).show();
        }

    }

    public void volleyPost(String postUrl, JSONObject postData, final String operation){
        Log.d(TAG, "VolleyPost: ");
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response);
                parseJSON(response,operation);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d(TAG, error.toString());
                Toast.makeText(LoginActivity.this, operation + " Failed",
                        Toast.LENGTH_LONG).show();
                hideProgressBar();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }


    private void volleyGet(String getUrl, final JSONObject postData, final String operation) {
        Log.d(TAG, "volleyGet: ");
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (operation.equals("Login")) {
                        Log.d(TAG, "Inside operation Login");
                        String token = (String) response.get("token");
                        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                        SharedPreferences.Editor myEdit;
                        myEdit = sharedPreferences.edit();
                        myEdit.putString("token", token);
                        myEdit.putString("username", username);
                        myEdit.commit();

                        Toast.makeText(LoginActivity.this,  operation + " Successful!!!",
                                Toast.LENGTH_LONG).show();
                        hideProgressBar();
                        openMainActivity();
                        return;
                    }
                    String username = (String) response.get("username");
                    SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("username", username);
                    myEdit.commit();
                    System.out.println(username);
                    Toast.makeText(LoginActivity.this, operation + " Successfully!!",
                            Toast.LENGTH_LONG).show();
                    hideProgressBar();
                    openMainActivity();
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, operation + " Failed",
                            Toast.LENGTH_LONG).show();
                    hideProgressBar();
                    deleteSharedPref();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                error.printStackTrace();
                Toast.makeText(LoginActivity.this, operation + " Failed",
                        Toast.LENGTH_LONG).show();
                hideProgressBar();
                deleteSharedPref();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                if (operation.equals("Login")) {
                    try {
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("Content-Type", "application/json");

                        params.put("username", username);
                        params.put("password", postData.getString("password"));
                        return params;
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                        e.printStackTrace();
                    }
                    return null;
                }

                SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                String token = sharedPreferences.getString("token", "");
                Map<String, String>  params = new HashMap<String, String>();
                String creds = String.format("%s:%s",token,"");
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                params.put("Authorization", auth);
                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    private void deleteSharedPref() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putString("token", "");
        myEdit.commit();
    }

    private void parseJSON(JSONObject objResponse, String operation) {
        Log.d(TAG, "parseJSON: ");
        try {
            String token = (String) objResponse.get("token");
            SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
            SharedPreferences.Editor myEdit;
            String temp = sharedPreferences.getString("token", "");
            if (!sharedPreferences.contains("token") || !temp.equals(token)) {
                myEdit = sharedPreferences.edit();
                myEdit.putString("token", token);
                myEdit.putString("username", username);
                myEdit.commit();
            }

            Toast.makeText(LoginActivity.this,  operation + " Successful!!!",
                    Toast.LENGTH_LONG).show();
            hideProgressBar();
            openMainActivity();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(LoginActivity.this, operation + " Failed",
                    Toast.LENGTH_LONG).show();
            hideProgressBar();
            deleteSharedPref();
        }
    }

}
