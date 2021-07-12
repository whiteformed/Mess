package com.example.mess;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.mess.ActivityHierarchy.BasicActivity;
import com.example.mess.Dialog.LoadingDialog;
import com.example.mess.Model.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.util.HashMap;

import javax.annotation.Nullable;

public class Mess extends Application {
    public static final String UID = "uid";
    public static final String TOKEN_ID = "tokenId";
    public static final String USERS = "users";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    public static final String STATUS = "status";
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";
    public static final String LAST_SEEN = "lastSeen";

    public static final String CHATS = "chats";
    public static final String FIRST = "first";
    public static final String FIRST_USERNAME = "firstUsername";
    public static final String FIRST_UNSEEN = "firstUnseen";
    public static final String SECOND = "second";
    public static final String SECOND_USERNAME = "secondUsername";
    public static final String SECOND_UNSEEN = "secondUnseen";
    public static final String LAST_MESSAGE = "lastMessage";
    public static final String LAST_MESSAGE_FROM = "lastMessageFrom";

    public static final String MESSAGES = "messages";
    public static final String MESSAGE = "message";
    public static final String SENDER = "sender";
    public static final String RECEIVER = "receiver";
    public static final String DELIVERED = "delivered";
    public static final String SEEN = "seen";
    public static final String DATE = "date";
    public static final String TYPE = "type";
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_PHOTO = "photo";
    public static final String KEY_PHOTO_ICON = ":@p@:";
    public static final String MESSAGE_PHOTOS = "message_photos";

    public static final String DEVICES = "devices";
    public static final String NOTIFICATIONS = "notifications";
    public static final String CH_MESSAGE = "messageChannel";

    public static final String PROFILE_PHOTOS = "profile_photos";
    public static final String THUMBS = "thumbs";
    public static final String PHOTO_URL = "photoUrl";
    public static final String PHOTO_THUMB_URL = "photoThumbUrl";

    public static final String ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL";
    public static final String ERROR_WRONG_PASSWORD = "ERROR_WRONG_PASSWORD";

    public static final String DIALOG = "dialog";
    public static final String DIALOG_LOADING = "dialog_loading";

    public static final String TAG_MESS = "Mess";
    public static final String TAG_PICASSO = "Picasso";

    public static final float DISABLED_BT_ALPHA = (float) 0.3;
    public static final int FADE_DURATION = 250;

    private BasicActivity currentActivity;
    private boolean isAppActive;

    private User loggedUser;

    private AlertDialog loadingDialog;
    private Handler loadingHandler = null;
    private Runnable loadingTimeOut;

    private String tokenId;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(instanceIdResult -> {
                    String token = instanceIdResult.getToken();
                    saveToken(token);
                });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        tokenId = sharedPreferences.getString(TOKEN_ID, null);
    }

    public View.OnFocusChangeListener getClickOutsideFocusListener() {
        return (v, hasFocus) -> {
            if (hasFocus) closeKeyboard(v);
        };
    }

    public void closeKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void disconnectUserDelayed() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (!isAppActive) updateUserStatus(OFFLINE);
        }, 100);
    }

    public void updateUserStatus(String status) {
        if (loggedUser == null) return;

        loggedUser.setStatus(status);

        DocumentReference userRef = FirebaseFirestore.getInstance()
                .document(USERS + '/' + loggedUser.getUid());

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put(LAST_SEEN, FieldValue.serverTimestamp());
        userMap.put(STATUS, status);

        userRef.update(userMap);
    }

    private void createNotificationChannels() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CH_MESSAGE,
                    getString(R.string.messages), NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.setDescription(getString(R.string.message_channel_description));

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void readLoggedUser(Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (firebaseUser == null) return;

        String loggedUid = firebaseUser.getUid();
        DocumentReference userRef = firestore.document(USERS + '/' + loggedUid);

        userRef.update(TOKEN_ID, tokenId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot == null) {
                if (!activity.isDestroyed() && !activity.isFinishing())
                    signOut(activity);
                return;
            }

            loggedUser = documentSnapshot.toObject(User.class);
            assert loggedUser != null;
            loggedUser.setUid(documentSnapshot.getId());

            if (isAppActive)
                updateUserStatus(ONLINE);
            else
                updateUserStatus(OFFLINE);

            loggedUser.setTokenId(tokenId);

            if (activity instanceof ChatActivity) {
                ((ChatActivity) activity).setLoggedUser(loggedUser);
                ((ChatActivity) activity).initializeUsernames();
            }
        });
    }

    public void signOut(Activity activity) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        if (firebaseUser == null) return;

        showLoadingDialog(R.string.logging_out);

        String loggedUid = firebaseUser.getUid();

        DocumentReference userRef = firestore.document(USERS + '/' + loggedUid);

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put(TOKEN_ID, null);
        userMap.put(STATUS, OFFLINE);
        userRef.update(userMap).addOnSuccessListener(aVoid -> {
            hideLoadingDialog();

            deleteCache(Mess.this);

            auth.signOut();
            loggedUser = null;

            if (activity instanceof MainActivity || activity instanceof ChatActivity) {
                activity.startActivity(new Intent(Mess.this, WelcomeActivity.class));
                activity.finish();
            }
        });
    }

    public void showLoadingDialog() {
        showLoadingDialog(R.string.loading);
    }

    public void showLoadingDialog(int loadingTextResId) {
        LoadingDialog loadingDialog = new LoadingDialog();
        Bundle args = new Bundle();
        args.putString(LoadingDialog.LOADING_TEXT, getString(loadingTextResId));
        loadingDialog.setArguments(args);
        FragmentManager fm = currentActivity.getSupportFragmentManager();
        loadingDialog.show(fm, DIALOG_LOADING);
    }

    public void hideLoadingDialog() {
        FragmentManager fm = currentActivity.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(DIALOG_LOADING);
        if (dialog != null) {
            LoadingDialog loadingDialog = (LoadingDialog) dialog;
            loadingDialog.dismiss();
        }
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            Log.e(TAG_MESS, e.getMessage(), e);
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children == null) return false;
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public void saveToken(String token) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(TOKEN_ID, token).apply();
        tokenId = token;
        if (loggedUser != null) loggedUser.setTokenId(token);
    }

    public void handleException(Exception exception) {
        if (exception == null) return;

        try {
            throw exception;
        } catch (FirebaseAuthInvalidCredentialsException e) {
            switch (e.getErrorCode()) {
                case ERROR_INVALID_EMAIL:
                    toast(R.string.invalid_email_error);
                    break;
                case ERROR_WRONG_PASSWORD:
                    toast(R.string.invalid_password_error);
                    break;
                default:
                    toast(e.getMessage());
                    break;
            }
        } catch (FirebaseAuthInvalidUserException e) {
            toast(R.string.user_not_found_error);
        } catch (FirebaseNetworkException e) {
            toast(R.string.network_error);
        } catch (FirebaseAuthUserCollisionException e) {
            toast(R.string.email_in_use_error);
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    public void toast(int messageResId) {
        toast(getString(messageResId));
    }

    public void toast(@Nullable String message) {
        if (message == null) return;
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.TOP, 0, 100);
        toast.show();
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(BasicActivity currentActivity) {
        this.currentActivity = currentActivity;
    }

    public void setAppActive(boolean appActive) {
        isAppActive = appActive;
    }

    public User getLoggedUser() {
        return loggedUser;
    }

    public String getTokenId() {
        return tokenId;
    }
}
