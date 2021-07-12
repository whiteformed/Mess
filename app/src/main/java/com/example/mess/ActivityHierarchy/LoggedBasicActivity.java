package com.example.mess.ActivityHierarchy;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.mess.Mess;

@SuppressLint("Registered")
public class LoggedBasicActivity extends BasicActivity {
    private Mess application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (Mess) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        application.updateUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.disconnectUserDelayed();
        // application.storeUserInfo();
    }
}
