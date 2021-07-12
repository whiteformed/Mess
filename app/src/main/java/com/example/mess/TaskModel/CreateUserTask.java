package com.example.mess.TaskModel;

public class CreateUserTask extends FirebaseTask {
    private String username;
    private String email;

    public CreateUserTask(boolean successful, Exception exception, String username, String email) {
        super(successful, exception);
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
