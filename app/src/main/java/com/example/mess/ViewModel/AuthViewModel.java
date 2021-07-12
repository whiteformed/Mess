package com.example.mess.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.mess.TaskModel.CreateUserTask;
import com.example.mess.TaskModel.FirebaseTask;
import com.example.mess.TaskModel.LoginTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthViewModel extends AndroidViewModel {
    public MutableLiveData<LoginTask> loginLiveData;
    public MutableLiveData<CreateUserTask> registerLiveData;
    public MutableLiveData<FirebaseTask> resetPwdLiveData;
    public MutableLiveData<FirebaseTask> updateEmailLiveData;
    public MutableLiveData<FirebaseTask> updatePasswordLiveData;
    public MutableLiveData<FirebaseTask> resendVerificationLiveData;

    private FirebaseAuth auth;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        loginLiveData = new MutableLiveData<>();
        registerLiveData = new MutableLiveData<>();
        resetPwdLiveData = new MutableLiveData<>();
        updateEmailLiveData = new MutableLiveData<>();
        updatePasswordLiveData = new MutableLiveData<>();
        resendVerificationLiveData = new MutableLiveData<>();
        auth = FirebaseAuth.getInstance();
    }

    public void signIn(String email, String password) {
        signIn(email, password, null, null);
    }

    public void signIn(String email, String password, String id, String result) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    FirebaseUser firebaseUser;
                    firebaseUser = auth.getCurrentUser();
                    LoginTask loginTask = new LoginTask(
                            task.isSuccessful(),
                            firebaseUser != null && firebaseUser.isEmailVerified(),
                            task.getException());
                    loginTask.setId(id);
                    loginTask.setResult(result);
                    loginLiveData.postValue(loginTask);
                });
    }

    public void register(String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    CreateUserTask createUserTask = new CreateUserTask(task.isSuccessful(), task.getException(), username, email);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        assert firebaseUser != null;
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username).build();
                        firebaseUser.updateProfile(profileUpdates);

                        firebaseUser.sendEmailVerification().addOnCompleteListener(sendVerification -> {
                            createUserTask.setSuccessful(sendVerification.isSuccessful());
                            createUserTask.setException(sendVerification.getException());
                            registerLiveData.postValue(createUserTask);
                        });
                    } else {
                        registerLiveData.postValue(createUserTask);
                    }
                });
    }

    public void resendVerification() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;
        firebaseUser.sendEmailVerification().addOnCompleteListener(task -> {
            FirebaseTask firebaseTask = new FirebaseTask(task.isSuccessful(), task.getException());
            resendVerificationLiveData.postValue(firebaseTask);
        });
    }

    public void resetPassword(String email) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            FirebaseTask resetPwdTask = new FirebaseTask(task.isSuccessful(), task.getException());
            resetPwdLiveData.postValue(resetPwdTask);
        });
    }

    public void updateEmail(String newEmail) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        assert firebaseUser != null;
        firebaseUser.updateEmail(newEmail).addOnCompleteListener(task -> {
            FirebaseTask updateEmailTask = new FirebaseTask(task.isSuccessful(), task.getException());
            updateEmailTask.setResult(newEmail);
            if (task.isSuccessful()) {
                firebaseUser.sendEmailVerification().addOnCompleteListener(sendVerification -> {
                    updateEmailTask.setSuccessful(sendVerification.isSuccessful());
                    updateEmailTask.setException(sendVerification.getException());
                    updateEmailLiveData.postValue(updateEmailTask);
                });
            } else {
                updateEmailLiveData.postValue(updateEmailTask);
            }
        });
    }

    public void updatePassword(String newPassword) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        assert firebaseUser != null;
        firebaseUser.updatePassword(newPassword).addOnCompleteListener(task -> {
            FirebaseTask updatePasswordTask = new FirebaseTask(task.isSuccessful(), task.getException());
            updatePasswordTask.setResult(newPassword);
            updatePasswordLiveData.postValue(updatePasswordTask);
        });
    }
}
