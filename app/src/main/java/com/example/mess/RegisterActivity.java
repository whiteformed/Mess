package com.example.mess;

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
import com.example.mess.ViewModel.FirestoreViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends BasicActivity {
    private Mess application;

    private EditText inputUsername, inputEmail, inputPassword;
    private Button btRegister;

    private AuthViewModel authViewModel;
    private FirestoreViewModel firestoreViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        application = (Mess) getApplication();

        inputUsername = findViewById(R.id.inputUsername);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btRegister = findViewById(R.id.btRegister);

        inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onClickRegister(btRegister);
                return true;
            }

            return false;
        });

        initViewModels();

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        rootLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) closeKeyboard();
        });
        mainLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) closeKeyboard();
        });
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(AuthViewModel.class);

        authViewModel.registerLiveData.observe(this, createUserTask -> {
            if (createUserTask.isChecked()) return;

            if (createUserTask.isSuccessful()) {
                firestoreViewModel.createUser(createUserTask);
            } else {
                application.hideLoadingDialog();
                btRegister.setEnabled(true);
                Exception e = createUserTask.getException();
                if (e instanceof FirebaseAuthUserCollisionException)
                    inputEmail.setBackground(getDrawable(R.drawable.input_bg_alt_error));
                application.handleException(e);
            }

            createUserTask.setChecked(true);
        });

        firestoreViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                .get(FirestoreViewModel.class);

        firestoreViewModel.createUserLiveData.observe(this, createUserTask -> {
            if (createUserTask.isChecked()) return;

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser firebaseUser = auth.getCurrentUser();
            assert firebaseUser != null;
            application.hideLoadingDialog();
            btRegister.setEnabled(true);
            if (createUserTask.isSuccessful()) {
                application.toast(R.string.register_success);
                finish();
            } else {
                application.handleException(createUserTask.getException());
                firebaseUser.delete();
            }
            auth.signOut();

            createUserTask.setChecked(true);
        });
    }

    public void onClickRegister(View view) {
        closeKeyboard();
        inputUsername.clearFocus();
        inputEmail.clearFocus();
        inputPassword.clearFocus();

        String username = inputUsername.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        inputUsername.setBackground(getDrawable(TextUtils.isEmpty(username) ? R.drawable.input_bg_alt_error : R.drawable.input_bg_alt));
        inputEmail.setBackground(getDrawable(TextUtils.isEmpty(email) ? R.drawable.input_bg_alt_error : R.drawable.input_bg_alt));
        inputPassword.setBackground(getDrawable(password.length() < 6 ? R.drawable.input_bg_alt_error : R.drawable.input_bg_alt));

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)) {
            application.toast(R.string.required_fields_error);
        } else if (password.length() < 6) {
            application.toast(R.string.password_char_error);
        } else {
            btRegister.setEnabled(false);
            application.showLoadingDialog(R.string.registering);
            authViewModel.register(username, email, password);
        }
    }

    public void onBackPressed(View view) {
        onBackPressed();
    }
}
