package com.example.mess;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.mess.ActivityHierarchy.BasicActivity;
import com.example.mess.ViewModel.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;

import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;
import static android.view.inputmethod.EditorInfo.IME_ACTION_NEXT;

public class AccountAccessActivity extends BasicActivity {
    private Mess application;

    private EditText inputEmail, inputPassword;
    private Button btSend;
    private RadioButton rbForgot, rbVerification;

    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_access);

        application = (Mess) getApplication();

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btSend = findViewById(R.id.btSend);

        inputEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onClickSend(btSend);
                return true;
            }

            return false;
        });

        inputPassword.setVisibility(View.GONE);

        TextView txtDescription = findViewById(R.id.txtDescription);
        rbForgot = findViewById(R.id.rbForgot);
        rbVerification = findViewById(R.id.rbVerification);

        rbForgot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btSend.setText(R.string.reset);
                txtDescription.setText(R.string.reset_description);
                inputEmail.setImeOptions(IME_ACTION_GO);
                inputPassword.setVisibility(View.GONE);
            }
        });

        rbVerification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btSend.setText(R.string.send);
                txtDescription.setText(R.string.verification_description);
                inputEmail.setImeOptions(IME_ACTION_NEXT);
                inputPassword.setVisibility(View.VISIBLE);
            }
        });

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
        FirebaseAuth auth = FirebaseAuth.getInstance();

        authViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(AuthViewModel.class);

        authViewModel.resetPwdLiveData.observe(this, resetPwdTask -> {
            if (resetPwdTask.isChecked()) return;

            application.hideLoadingDialog();
            btSend.setEnabled(true);
            if (resetPwdTask.isSuccessful()) {
                application.toast(R.string.password_reset_mail_sent);
                finish();
            } else {
                application.handleException(resetPwdTask.getException());
            }

            resetPwdTask.setChecked(true);
        });

        authViewModel.loginLiveData.observe(this, loginTask -> {
            if (loginTask.isChecked()) return;

            if (loginTask.isSuccessful()) {
                if (loginTask.isEmailVerified()) {
                    auth.signOut();
                    application.hideLoadingDialog();
                    btSend.setEnabled(true);
                    application.toast(R.string.already_verified_error);
                } else {
                    authViewModel.resendVerification();
                }
            } else {
                auth.signOut();
                application.hideLoadingDialog();
                btSend.setEnabled(true);
                application.handleException(loginTask.getException());
            }

            loginTask.setChecked(true);
        });

        authViewModel.resendVerificationLiveData.observe(this, firebaseTask -> {
            if (firebaseTask.isChecked()) return;

            auth.signOut();
            application.hideLoadingDialog();
            btSend.setEnabled(true);
            if (firebaseTask.isSuccessful()) {
                application.toast(R.string.verification_sent);
                finish();
            } else {
                application.handleException(firebaseTask.getException());
            }

            firebaseTask.setChecked(true);
        });
    }

    public void onClickSend(View view) {
        closeKeyboard();
        inputEmail.clearFocus();

        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (TextUtils.isEmpty(email)) return;
        if (rbVerification.isChecked() && TextUtils.isEmpty(password)) return;

        btSend.setEnabled(false);
        application.showLoadingDialog();

        if (rbForgot.isChecked())
            authViewModel.resetPassword(email);
        else if (rbVerification.isChecked())
            authViewModel.signIn(email, password);
    }

    public void onBackPressed(View view) {
        onBackPressed();
    }
}
