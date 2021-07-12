package com.example.mess;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.mess.ActivityHierarchy.BasicActivity;
import com.example.mess.ViewModel.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends BasicActivity {
    private Mess application;
    private Context context;

    private EditText inputEmail, inputPassword;
    private Button btLogin;

    private FirebaseAuth auth;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        context = this;
        application = (Mess) getApplication();

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btLogin = findViewById(R.id.btLogin);

        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onClickLogin(btLogin);
                return true;
            }

            return false;
        });

        auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (savedInstanceState == null && firebaseUser != null && firebaseUser.isEmailVerified()) {
            startActivity(new Intent(context, MainActivity.class));
            finish();
        }

        initAuthViewModel();

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        rootLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) closeKeyboard();
        });
        mainLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) closeKeyboard();
        });
    }

    private void initAuthViewModel() {
        authViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(AuthViewModel.class);

        authViewModel.loginLiveData.observe(this, loginTask -> {
            if (loginTask.isChecked()) return;

            application.hideLoadingDialog();
            btLogin.setEnabled(true);
            if (loginTask.isSuccessful()) {
                if (loginTask.isEmailVerified()) {
                    startActivity(new Intent(context, MainActivity.class));
                    finish();
                } else {
                    auth.signOut();
                    application.toast(R.string.verification_error);
                }
            } else {
                application.handleException(loginTask.getException());
            }

            loginTask.setChecked(true);
        });
    }

    public void onClickLogin(View view) {
        application.closeKeyboard(view);
        inputEmail.clearFocus();
        inputPassword.clearFocus();

        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            btLogin.setEnabled(false);
            application.showLoadingDialog(R.string.logging_in);
            authViewModel.signIn(email, password);
        }
    }

    public void onClickRegister(View view) {
        startActivity(new Intent(context, RegisterActivity.class));
    }

    public void onClickAccountAccess(View view) {
        startActivity(new Intent(context, AccountAccessActivity.class));
    }
}
