package com.example.mess.TaskModel;

public class FirebaseTask {
    private String id;
    private boolean checked;
    private boolean successful;
    private Exception exception;
    private String result;

    public FirebaseTask(boolean successful, Exception exception) {
        this.successful = successful;
        this.exception = exception;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public Exception getException() {
        return exception;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
