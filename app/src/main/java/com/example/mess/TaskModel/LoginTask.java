package com.example.mess.TaskModel;

public class LoginTask extends FirebaseTask {
    private boolean emailVerified;

    public LoginTask(boolean successful, boolean emailVerified, Exception exception) {
        super(successful, exception);
        this.emailVerified = emailVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
