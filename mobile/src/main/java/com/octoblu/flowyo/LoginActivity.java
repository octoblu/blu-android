package com.octoblu.flowyo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Set;

public class LoginActivity extends Activity {
    private final static String TAG = "FlowYo:login";
    final static String PREFERENCES_FILE_NAME = "meshblu_credentials";
    private final static String LOGIN_URL = "http://staging.octoblu.com/static/auth-login.html";
    public static final String UUID_KEY = "uuid";
    public static final String TOKEN_KEY = "token";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final WebView loginView = (WebView) findViewById(R.id.login_view);
        loginView.getSettings().setJavaScriptEnabled(true);
        loginView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                Uri uri = Uri.parse(url);
                Set<String> queryParameters = uri.getQueryParameterNames();
                if(!queryParameters.contains("uuid") || !queryParameters.contains("token")) {
                    return;
                }

                String uuid = uri.getQueryParameter("uuid");
                String token = uri.getQueryParameter("token");
                if(uuid.equals("undefined") || token.equals("undefined")){
                    loginView.loadUrl(LOGIN_URL);
                    Toast.makeText(getApplicationContext(), "Invalid username or password", Toast.LENGTH_LONG).show();
                    return;
                }

                SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE_NAME, 0);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(UUID_KEY, uuid);
                editor.putString(TOKEN_KEY, token);
                editor.commit();
                finish();
            }
        });
        loginView.loadUrl(LOGIN_URL);
    }
}
