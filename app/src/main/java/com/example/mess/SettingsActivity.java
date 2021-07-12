package com.example.mess;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.mess.ActivityHierarchy.LoggedBasicActivity;
import com.example.mess.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import id.zelory.compressor.Compressor;

import static com.example.mess.Mess.CHATS;
import static com.example.mess.Mess.DISABLED_BT_ALPHA;
import static com.example.mess.Mess.EMAIL;
import static com.example.mess.Mess.FADE_DURATION;
import static com.example.mess.Mess.FIRST_USERNAME;
import static com.example.mess.Mess.ONLINE;
import static com.example.mess.Mess.PASSWORD;
import static com.example.mess.Mess.PHOTO_THUMB_URL;
import static com.example.mess.Mess.PHOTO_URL;
import static com.example.mess.Mess.PROFILE_PHOTOS;
import static com.example.mess.Mess.SECOND_USERNAME;
import static com.example.mess.Mess.TAG_MESS;
import static com.example.mess.Mess.TAG_PICASSO;
import static com.example.mess.Mess.THUMBS;
import static com.example.mess.Mess.USERNAME;
import static com.example.mess.Mess.USERS;

public class SettingsActivity extends LoggedBasicActivity {
    public static final int REQ_IMAGE = 1;

    public static final String CONFIRM_PASS_FOR_EMAIL = "confirm_pass_for_email";
    public static final String CONFIRM_PASS_FOR_PASS = "confirm_pass_for_pass";

    private Mess application;
    private Context context;

    private ImageView imgProfile;
    private TextView txtUsername, txtEmail;
    private Button btRemovePP;
    private ImageButton btEditEmail, btEditUsername;
    private ProgressBar emailProgressBar, profileProgressBar, usernameProgressBar;

    private StorageReference storageReference;

    private User loggedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = this;
        application = (Mess) getApplication();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getString(R.string.settings));

        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        btRemovePP = findViewById(R.id.btRemovePP);
        btEditEmail = findViewById(R.id.btEditEmail);
        btEditUsername = findViewById(R.id.btEditUsername);
        emailProgressBar = findViewById(R.id.emailProgressBar);
        usernameProgressBar = findViewById(R.id.usernameProgressBar);
        imgProfile = findViewById(R.id.imgProfile);
        profileProgressBar = findViewById(R.id.profileProgressBar);

        imgProfile.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));

        loggedUser = application.getLoggedUser();
        if (loggedUser != null) {
            txtUsername.setText(loggedUser.getUsername());
            txtEmail.setText(loggedUser.getEmail());

            String photoThumbUrl = loggedUser.getPhotoThumbUrl();

            loadProfilePhoto(photoThumbUrl, true, 0);
        }

        storageReference = FirebaseStorage.getInstance().getReference();
    }

    private void loadProfilePhoto(String photoThumbUrl, boolean offlineMode, int attempt) {
        if (photoThumbUrl == null) {
            btRemovePP.setEnabled(false);
            btRemovePP.setAlpha(DISABLED_BT_ALPHA);
            return;
        }

        imgProfile.setAlpha((float) 0);
        profileProgressBar.setVisibility(View.VISIBLE);

        RequestCreator requestCreator = Picasso.get().load(photoThumbUrl);

        if (offlineMode)
            requestCreator = requestCreator.networkPolicy(NetworkPolicy.OFFLINE);

        requestCreator.into(imgProfile, new Callback() {
            @Override
            public void onSuccess() {
                imgProfile.animate().alpha(1).setDuration(FADE_DURATION);
                profileProgressBar.setVisibility(View.GONE);
                imgProfile.clearColorFilter();
            }

            @Override
            public void onError(Exception e) {
                if (attempt < 3) {
                    loadProfilePhoto(photoThumbUrl, false, attempt + 1);
                    return;
                }

                Log.e(TAG_PICASSO, e.getMessage(), e);
                imgProfile.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                imgProfile.setImageDrawable(getDrawable(R.drawable.account));
                imgProfile.animate().alpha(1).setDuration(FADE_DURATION);
                profileProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }

    public void onClickEditUsername(View view) {
        showEditDialog(InputType.TYPE_CLASS_TEXT, R.string.change_username_hint,
                loggedUser.getUsername(), R.string.change_username, USERNAME, null);
    }

    public void onClickEditEmail(View view) {
        showEditDialog(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                R.string.change_email_hint, loggedUser.getEmail(), R.string.change_email, EMAIL, null);
    }

    private void showConfirmPasswordDialogForEmail(String newEmail) {
        showEditDialog(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                R.string.password, "", R.string.confirm_password, CONFIRM_PASS_FOR_EMAIL, newEmail);
    }

    public void onClickEditPassword(View view) {
        showEditDialog(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                R.string.change_password_hint, "", R.string.change_password, PASSWORD, null);
    }

    private void showConfirmPasswordDialogForPassword(String newPassword) {
        showEditDialog(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                R.string.password, "", R.string.confirm_old_password, CONFIRM_PASS_FOR_PASS, newPassword);
    }

    public void onClickRemovePP(View view) {
        btRemovePP.setEnabled(false);
        btRemovePP.setAlpha(DISABLED_BT_ALPHA);

        String loggedUid = loggedUser.getUid();

        imgProfile.animate().alpha(0).setDuration(FADE_DURATION);
        profileProgressBar.setVisibility(View.VISIBLE);

        StorageReference ppRef = storageReference.child(PROFILE_PHOTOS)
                .child(loggedUid + ".jpg");

        ppRef.delete().addOnSuccessListener(aVoid -> {
            StorageReference ppThumbRef = storageReference.child(PROFILE_PHOTOS).child(THUMBS)
                    .child(loggedUid + ".jpg");
            ppThumbRef.delete().addOnSuccessListener(aVoid1 -> {
                DocumentReference userRef = FirebaseFirestore.getInstance()
                        .document(USERS + '/' + loggedUid);

                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put(PHOTO_URL, null);
                userMap.put(PHOTO_THUMB_URL, null);

                userRef.update(userMap).addOnSuccessListener(aVoid2 -> {
                    imgProfile.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                    imgProfile.setImageDrawable(getDrawable(R.drawable.account));
                    imgProfile.animate().alpha(1).setDuration(FADE_DURATION);
                    profileProgressBar.setVisibility(View.GONE);

                    Toast.makeText(context, getString(R.string.remove_profile_photo_success), Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    public void onClickEditPP(View view) {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(galleryIntent, getString(R.string.choose_image)), REQ_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_IMAGE:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    Uri imageUri = data.getData();

                    CropImage.activity(imageUri)
                            .setAspectRatio(1, 1)
                            .start(this);
                }
                break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                assert result != null;
                if (resultCode == RESULT_OK) {
                    application.showLoadingDialog(R.string.loading);

                    Uri resultUri = result.getUri();

                    savePhoto(resultUri);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception e = result.getError();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void savePhoto(Uri photoUri) {
        StorageReference ppRef = storageReference.child(PROFILE_PHOTOS)
                .child(loggedUser.getUid() + ".jpg");

        ppRef.putFile(photoUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updatePhoto(ppRef, photoUri);
            } else {
                Exception e = task.getException();
                uploadFailed(e);
            }
        });
    }

    private void updatePhoto(StorageReference ppRef, Uri photoUri) {
        ppRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String photoUrl = uri.toString();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference userRef = firestore
                    .document(USERS + '/' + loggedUser.getUid());
            userRef.update(PHOTO_URL, photoUrl).addOnSuccessListener(aVoid -> {
                loggedUser.setPhotoUrl(photoUrl);
                saveThumbnail(photoUri);
            });
        });
    }

    private void saveThumbnail(Uri photoUri) {
        try {
            String path = photoUri.getPath();
            assert path != null;
            File thumbFile = new File(path);

            Bitmap thumbBitmap = new Compressor(this)
                    .setMaxWidth(192)
                    .setMaxHeight(192)
                    .setQuality(75)
                    .compressToBitmap(thumbFile);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] thumbBytes = stream.toByteArray();

            StorageReference ppThumbRef = storageReference.child(PROFILE_PHOTOS)
                    .child(THUMBS).child(loggedUser.getUid() + ".jpg");

            ppThumbRef.putBytes(thumbBytes).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    updateThumbnail(ppThumbRef);
                } else {
                    Exception e = task.getException();
                    uploadFailed(e);
                }
            });
        } catch (IOException e) {
            Log.e(TAG_MESS, e.getMessage(), e);
        }
    }

    private void updateThumbnail(StorageReference ppThumbRef) {
        imgProfile.animate().alpha(0).setDuration(FADE_DURATION);
        profileProgressBar.setVisibility(View.VISIBLE);

        ppThumbRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String thumbUrl = uri.toString();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference userRef = firestore
                    .document(USERS + '/' + loggedUser.getUid());
            userRef.update(PHOTO_THUMB_URL, thumbUrl).addOnSuccessListener(aVoid -> {
                loggedUser.setPhotoThumbUrl(thumbUrl);
                application.hideLoadingDialog();
                btRemovePP.setEnabled(true);
                btRemovePP.setAlpha(1);
                Toast.makeText(context, getString(R.string.change_profile_photo_success), Toast.LENGTH_SHORT).show();

                loadProfilePhoto(thumbUrl, false, 0);
            });
        });
    }

    private void uploadFailed(Exception e) {
        application.hideLoadingDialog();
        application.handleException(e);
    }

    @SuppressLint("InflateParams")
    private void showEditDialog(int inputTypeFlags, int inputHintResource, String inputText,
                                int dialogTitleResource, String methodKey, String alternateValue) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_prompt, null);
        EditText input = dialogView.findViewById(R.id.input);
        input.setInputType(inputTypeFlags);
        input.setHint(inputHintResource);
        input.setText(inputText);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(dialogTitleResource)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String inputValue = input.getText().toString();
                    applyMethod(methodKey, inputValue, alternateValue);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        Window dialogWindow = alertDialog.getWindow();

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputValue = input.getText().toString();
                applyMethod(methodKey, inputValue, alternateValue);
                alertDialog.dismiss();
                return true;
            }

            return false;
        });

        input.requestFocus();
        input.selectAll();

        assert dialogWindow != null;
        dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        alertDialog.show();
    }

    private void applyMethod(String methodKey, String inputValue, String alternateValue) {
        switch (methodKey) {
            case USERNAME:
                updateUsername(inputValue);
                break;
            case EMAIL:
                updateEmail(inputValue);
                break;
            case PASSWORD:
                updatePassword(inputValue);
                break;
            case CONFIRM_PASS_FOR_EMAIL:
                confirmPassword(alternateValue, EMAIL, inputValue);
                break;
            case CONFIRM_PASS_FOR_PASS:
                confirmPassword(alternateValue, PASSWORD, inputValue);
                break;
        }
    }

    private void updateUsername(String newUsername) {
        if (TextUtils.isEmpty(newUsername)) return;

        toggleImageButton(btEditUsername, usernameProgressBar, false);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.document(USERS + '/' + loggedUser.getUid());

        userRef.update(USERNAME, newUsername)
                .addOnCompleteListener(task -> {
                    toggleImageButton(btEditUsername, usernameProgressBar, true);

                    if (task.isSuccessful()) {
                        txtUsername.setText(newUsername);
                        loggedUser.setUsername(newUsername);
                        updateChats(newUsername);
                    } else {
                        application.handleException(task.getException());
                    }
                });

    }

    private void updateChats(String newUsername) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference allChats = firestore.collection(CHATS);

        allChats.get().addOnSuccessListener(queryDocumentSnapshots -> {
            String loggedUid = loggedUser.getUid();
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                String chatId = documentSnapshot.getId();

                if (!chatId.contains(loggedUid)) continue;

                DocumentReference docRef = documentSnapshot.getReference();

                String[] UIDs = chatId.split("-");

                if (loggedUid.equals(UIDs[0]))
                    docRef.update(FIRST_USERNAME, newUsername);
                else
                    docRef.update(SECOND_USERNAME, newUsername);
            }
        });
    }

    private void updateEmail(String newEmail) {
        if (TextUtils.isEmpty(newEmail)) return;

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.document(USERS + '/' + loggedUser.getUid());

        toggleImageButton(btEditEmail, emailProgressBar, false);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        firebaseUser.updateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userRef.update(EMAIL, newEmail).addOnSuccessListener(aVoid -> {
                    toggleImageButton(btEditEmail, emailProgressBar, true);
                    txtEmail.setText(newEmail);
                    loggedUser.setEmail(newEmail);
                });
            } else {
                toggleImageButton(btEditEmail, emailProgressBar, true);
                try {
                    Exception e = task.getException();
                    if (e != null) throw e;
                } catch (FirebaseAuthRecentLoginRequiredException e) {
                    showConfirmPasswordDialogForEmail(newEmail);
                } catch (Exception e) {
                    application.handleException(e);
                }
            }
        });
    }

    private void updatePassword(String newPassword) {
        if (TextUtils.isEmpty(newPassword)) return;

        if (newPassword.length() < 6) {
            Toast.makeText(this, getString(R.string.password_char_error), Toast.LENGTH_SHORT).show();
            return;
        }

        application.showLoadingDialog(R.string.loading);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        firebaseUser.updatePassword(newPassword).addOnCompleteListener(task -> {
            application.hideLoadingDialog();

            if (task.isSuccessful()) {
                Toast.makeText(context, getString(R.string.change_password_success), Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Exception e = task.getException();
                    if (e != null) throw e;
                } catch (FirebaseAuthRecentLoginRequiredException e) {
                    showConfirmPasswordDialogForPassword(newPassword);
                } catch (Exception e) {
                    application.handleException(e);
                }
            }
        });
    }

    private void toggleImageButton(ImageButton button, ProgressBar progressBar, boolean state) {
        if (state) {
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private void confirmPassword(String newValue, String confirmationType,
                                 String password) {
        switch (confirmationType) {
            case CONFIRM_PASS_FOR_EMAIL:
                toggleImageButton(btEditEmail, emailProgressBar, false);
                break;
            case CONFIRM_PASS_FOR_PASS:
                application.showLoadingDialog(R.string.loading);
                break;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(loggedUser.getEmail(), password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        switch (confirmationType) {
                            case EMAIL:
                                updateEmail(newValue);
                            case PASSWORD:
                                updatePassword(newValue);
                        }
                    } else {
                        switch (confirmationType) {
                            case CONFIRM_PASS_FOR_EMAIL:
                                toggleImageButton(btEditEmail, emailProgressBar, true);
                                break;
                            case CONFIRM_PASS_FOR_PASS:
                                application.hideLoadingDialog();
                                break;
                        }

                        Toast.makeText(context, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        application.setCurrentActivity(this);
        application.setAppActive(true);
        super.onResume();
        application.updateUserStatus(ONLINE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.setCurrentActivity(null);
        application.setAppActive(false);
        application.disconnectUserDelayed();
    }
}
