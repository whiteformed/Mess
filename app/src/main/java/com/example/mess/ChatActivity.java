package com.example.mess;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mess.ActivityHierarchy.LoggedBasicActivity;
import com.example.mess.Adapter.MessageAdapter;
import com.example.mess.Model.ChatMessage;
import com.example.mess.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.mess.Mess.CHATS;
import static com.example.mess.Mess.DATE;
import static com.example.mess.Mess.DELIVERED;
import static com.example.mess.Mess.DEVICES;
import static com.example.mess.Mess.FADE_DURATION;
import static com.example.mess.Mess.FIRST;
import static com.example.mess.Mess.FIRST_UNSEEN;
import static com.example.mess.Mess.FIRST_USERNAME;
import static com.example.mess.Mess.KEY_PHOTO_ICON;
import static com.example.mess.Mess.LAST_MESSAGE;
import static com.example.mess.Mess.LAST_MESSAGE_FROM;
import static com.example.mess.Mess.MESSAGE;
import static com.example.mess.Mess.MESSAGES;
import static com.example.mess.Mess.MESSAGE_PHOTOS;
import static com.example.mess.Mess.NOTIFICATIONS;
import static com.example.mess.Mess.OFFLINE;
import static com.example.mess.Mess.ONLINE;
import static com.example.mess.Mess.PHOTO_THUMB_URL;
import static com.example.mess.Mess.PHOTO_URL;
import static com.example.mess.Mess.RECEIVER;
import static com.example.mess.Mess.SECOND;
import static com.example.mess.Mess.SECOND_UNSEEN;
import static com.example.mess.Mess.SECOND_USERNAME;
import static com.example.mess.Mess.SEEN;
import static com.example.mess.Mess.SENDER;
import static com.example.mess.Mess.TAG_PICASSO;
import static com.example.mess.Mess.TYPE;
import static com.example.mess.Mess.TYPE_PHOTO;
import static com.example.mess.Mess.TYPE_TEXT;
import static com.example.mess.Mess.UID;
import static com.example.mess.Mess.USERNAME;
import static com.example.mess.Mess.USERS;

public class ChatActivity extends LoggedBasicActivity {
    public static final int REQ_IMAGE = 1;

    private FirebaseFirestore firestore;

    private Context context;
    private Mess application;

    private ImageView imgProfileThumb;
    private TextView txtStatus;
    private EditText inputText;
    private ProgressBar progressBar, profileProgressBar;

    private ArrayList<ChatMessage> messages;
    private Stack<String> newMessages;
    private MessageAdapter messageAdapter;
    private RecyclerView recyclerView;

    private int recyclerViewOffset;

    private User loggedUser, targetUser;
    private String targetUid, targetUsername, loggedUid, chatId, userState, firstUsername, secondUsername;

    private StorageReference storageReference;

    private boolean isActivityActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        application = (Mess) getApplication();
        context = this;

        progressBar = findViewById(R.id.progressBar);
        profileProgressBar = findViewById(R.id.profileProgressBar);

        TextView txtUsername = findViewById(R.id.txtUsername);
        targetUsername = intent.getStringExtra(USERNAME);
        txtUsername.setText(targetUsername);

        targetUid = intent.getStringExtra(UID);

        txtStatus = findViewById(R.id.txtStatus);
        inputText = findViewById(R.id.inputText);

        txtStatus.setVisibility(View.GONE);

        LinearLayout mainLayout = findViewById(R.id.mainLayout);
        mainLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                if (recyclerViewOffset <= 0) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("");

        imgProfileThumb = findViewById(R.id.imgProfileThumb);

        imgProfileThumb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.account_thumb));
        imgProfileThumb.setColorFilter(ContextCompat.getColor(context, android.R.color.white));

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        loggedUid = firebaseUser.getUid();

        userState = loggedUid.compareTo(targetUid) > 0 ? FIRST : SECOND;

        chatId = userState.equals(FIRST) ?
                loggedUid + '-' + targetUid : targetUid + '-' + loggedUid;

        loggedUser = application.getLoggedUser();

        if (loggedUser == null) application.readLoggedUser(this);
        else if (loggedUser.getUsername() != null) initializeUsernames();

        DocumentReference targetUserDocument = firestore.document(USERS + '/' + targetUid);

        targetUserDocument.addSnapshotListener((documentSnapshot, e) -> {
            if (documentSnapshot == null) return;

            User oldUser = targetUser;

            targetUser = documentSnapshot.toObject(User.class);
            txtStatus.setVisibility(View.VISIBLE);
            assert targetUser != null;
            txtUsername.setText(targetUser.getUsername());
            switch (targetUser.getStatus()) {
                case ONLINE:
                    txtStatus.setText(R.string.online);
                    break;
                case OFFLINE:
                    Date date = targetUser.getLastSeen();
                    if (date == null) {
                        txtStatus.setText(R.string.offline);
                    } else {
                        SimpleDateFormat sdf;
                        String strDate;
                        if (DateUtils.isToday(date.getTime())) {
                            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            strDate = sdf.format(date);
                        } else {
                            Calendar calendarNow = Calendar.getInstance();
                            Calendar calendarChat = Calendar.getInstance();
                            calendarChat.setTime(date);

                            if (calendarNow.get(Calendar.YEAR) == calendarChat.get(Calendar.YEAR)
                                    && calendarNow.get(Calendar.DAY_OF_YEAR) - 1 == calendarChat.get(Calendar.DAY_OF_YEAR)) {
                                strDate = context.getString(R.string.yesterday);
                            } else {
                                sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                                strDate = sdf.format(date);
                            }

                        }

                        String strStatus = getString(R.string.last_seen) + ": " + strDate;
                        txtStatus.setText(strStatus);
                    }
                    break;
            }

            String photoThumbUrl = targetUser.getPhotoThumbUrl();

            if (oldUser == null || oldUser.getPhotoThumbUrl() == null ||
                    !oldUser.getPhotoThumbUrl().equals(photoThumbUrl)) {
                updateProfileThumb(photoThumbUrl);
            }
        });

        String photoThumbUrl = intent.getStringExtra(PHOTO_THUMB_URL);
        if (photoThumbUrl != null) updateProfileThumb(photoThumbUrl);

        recyclerView = findViewById(R.id.recyclerView);

        DefaultItemAnimator animator = ((DefaultItemAnimator) recyclerView.getItemAnimator());
        assert animator != null;
        animator.setSupportsChangeAnimations(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messages = new ArrayList<>();
        newMessages = new Stack<>();
        messageAdapter = new MessageAdapter(this, messages);
        recyclerView.setAdapter(messageAdapter);

        readMessages();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                recyclerViewOffset = recyclerView.computeVerticalScrollRange()
                        - recyclerView.computeVerticalScrollOffset() - recyclerView.getHeight();
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (progressBar.getVisibility() == View.VISIBLE) return false;
                messageAdapter.setPendingImageLoads(-1);
                recyclerView.removeOnItemTouchListener(this);
                return true;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public void initializeUsernames() {
        switch (userState) {
            case FIRST:
                firstUsername = loggedUser.getUsername();
                secondUsername = targetUsername;
                break;
            case SECOND:
                firstUsername = targetUsername;
                secondUsername = loggedUser.getUsername();
                break;
        }
    }

    private void updateProfileThumb(String photoThumbUrl) {
        imgProfileThumb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.account_thumb));
        imgProfileThumb.setColorFilter(ContextCompat.getColor(context, android.R.color.white));

        if (photoThumbUrl == null) return;

        imgProfileThumb.setAlpha((float) 0);
        profileProgressBar.setVisibility(View.VISIBLE);
        Picasso.get().load(photoThumbUrl)
                .into(imgProfileThumb, new Callback() {
                    @Override
                    public void onSuccess() {
                        imgProfileThumb.setAlpha((float) 1);
                        profileProgressBar.setVisibility(View.GONE);
                        imgProfileThumb.clearColorFilter();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG_PICASSO, e.getMessage(), e);
                        imgProfileThumb.setColorFilter(ContextCompat.getColor(context, android.R.color.white));
                        imgProfileThumb.setImageDrawable(context.getDrawable(R.drawable.account_thumb));
                        imgProfileThumb.animate().alpha(1).setDuration(FADE_DURATION);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_IMAGE:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    Uri imageUri = data.getData();

                    CropImage.activity(imageUri).start(this);
                }
                break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                assert result != null;
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    uploadPhoto(resultUri);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception e = result.getError();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void onClickSendPhoto(View view) {
        if (loggedUser == null || progressBar.getVisibility() == View.VISIBLE) return;

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(galleryIntent, getString(R.string.choose_image)), REQ_IMAGE);
    }

    private void uploadPhoto(Uri photoUri) {
        String uniqueId = UUID.randomUUID().toString();

        insertNewMessage(new ChatMessage
                (uniqueId, false, loggedUid, targetUid, KEY_PHOTO_ICON, TYPE_PHOTO, null));

        StorageReference photoRef = storageReference.child(MESSAGE_PHOTOS)
                .child(uniqueId + ".jpg");

        photoRef.putFile(photoUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String photoUrl = uri.toString();
                    sendMessage(uniqueId, KEY_PHOTO_ICON, TYPE_PHOTO, photoUrl);
                });
            } else {
                application.handleException(task.getException());
            }
        });
    }

    public void onClickSend(View view) {
        if (loggedUser == null || progressBar.getVisibility() == View.VISIBLE) return;

        String message = inputText.getText().toString();

        inputText.setText("");

        if (TextUtils.isEmpty(message)) return;

        String uniqueId = UUID.randomUUID().toString();
        insertNewMessage(new ChatMessage
                (uniqueId, false, loggedUid, targetUid, message, TYPE_TEXT, null));
        sendMessage(uniqueId, message, TYPE_TEXT, null);
    }

    private void sendMessage(String messageId, String message, String type, String photoUrl) {
        updateChat(message);

        DocumentReference messageRef = firestore
                .document(CHATS + '/' + chatId + '/' + MESSAGES + '/' + messageId);

        Map<String, Object> messageMap = new HashMap<>();

        messageMap.put(SENDER, loggedUid);
        messageMap.put(RECEIVER, targetUid);
        messageMap.put(MESSAGE, message);
        messageMap.put(TYPE, type);
        messageMap.put(PHOTO_URL, photoUrl);
        messageMap.put(DELIVERED, false);
        messageMap.put(SEEN, false);
        messageMap.put(DATE, FieldValue.serverTimestamp());

        messageRef.set(messageMap);

        if (targetUser == null) return;
        String targetToken = targetUser.getTokenId();
        if (targetToken == null) return;

        DocumentReference notificationRef =
                firestore.document(DEVICES + '/' + targetToken + '/' + NOTIFICATIONS + '/' + messageId);

        Map<String, Object> notificationMap = new HashMap<>();

        notificationMap.put(SENDER, loggedUid);
        notificationMap.put(USERNAME, loggedUser.getUsername());
        notificationMap.put(MESSAGE, message);

        notificationRef.set(notificationMap);
    }

    private void updateChat(String message) {
        DocumentReference chatRef = firestore.document(CHATS + '/' + chatId);

        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot chatSnapshot = task.getResult();

                String keyTargetUnseen = userState.equals(FIRST) ? SECOND_UNSEEN : FIRST_UNSEEN;
                String keyUserUnseen = userState.equals(FIRST) ? FIRST_UNSEEN : SECOND_UNSEEN;

                Map<String, Object> chatMap = new HashMap<>();

                chatMap.put(LAST_MESSAGE, message);
                chatMap.put(LAST_MESSAGE_FROM, userState);
                chatMap.put(DATE, FieldValue.serverTimestamp());

                if (chatSnapshot != null && chatSnapshot.exists()) {
                    int unseen = Long.valueOf((long) chatSnapshot.get(keyTargetUnseen)).intValue();
                    chatMap.put(keyTargetUnseen, ++unseen);

                    chatRef.update(chatMap);
                } else {
                    chatMap.put(FIRST_USERNAME, firstUsername);
                    chatMap.put(SECOND_USERNAME, secondUsername);
                    chatMap.put(keyUserUnseen, 0);
                    chatMap.put(keyTargetUnseen, 1);

                    chatRef.set(chatMap);
                }
            }
        });
    }

    private void readMessages() {

        CollectionReference allMessages = firestore
                .collection(CHATS + '/' + chatId + '/' + MESSAGES);

        Query orderedMessages = allMessages.orderBy(DATE, Query.Direction.ASCENDING);
        orderedMessages.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null || queryDocumentSnapshots == null) return;

            progressBar.setVisibility(View.GONE);

            for (DocumentChange documentChange : queryDocumentSnapshots.getDocumentChanges()) {
                DocumentSnapshot document = documentChange.getDocument();
                DocumentReference docRef = document.getReference();
                ChatMessage chatMessage = document.toObject(ChatMessage.class);
                String id = document.getId();
                chatMessage.setId(id);
                boolean sentFromUser = loggedUid.equals(chatMessage.getSender());

                if (!chatMessage.isDelivered()) {
                    docRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.getMetadata().isFromCache()) return;
                        docRef.update(DELIVERED, true);
                    });
                }

                switch (documentChange.getType()) {
                    case ADDED:
                        if (!sentFromUser) {
                            if (isActivityActive) {
                                docRef.update(SEEN, true);
                                DocumentReference notificationRef =
                                        firestore.document(DEVICES + '/' +
                                                application.getTokenId() + '/' + NOTIFICATIONS + '/' + id);
                                notificationRef.delete();
                                resetUnseen();
                            } else {
                                newMessages.push(id);
                            }
                        }

                        if (!sentFromUser || !changeExistingMessage(chatMessage))
                            insertNewMessage(chatMessage);
                        break;

                    case MODIFIED:
                        changeExistingMessage(chatMessage);

                        break;
                }
            }
        });
    }

    private void insertNewMessage(ChatMessage chatMessage) {
        int newIndex = messages.size();
        messages.add(chatMessage);
        messageAdapter.notifyItemInserted(newIndex);
        recyclerView.scrollToPosition(newIndex);
    }

    private boolean changeExistingMessage(ChatMessage chatMessage) {
        String messageId = chatMessage.getId();
        int index = getMessageIndex(messageId);
        if (index == -1) return false;
        messages.set(index, chatMessage);
        messageAdapter.notifyItemChanged(index);
        recyclerView.scrollToPosition(messages.size() - 1);
        return true;
    }

    private void resetUnseen() {
        DocumentReference chatRef = firestore.document(CHATS + '/' + chatId);

        switch (userState) {
            case FIRST:
                chatRef.update(FIRST_UNSEEN, 0);
                break;
            case SECOND:
                chatRef.update(SECOND_UNSEEN, 0);
                break;
        }
    }

    private int getMessageIndex(String messageId) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messageId.equals(messages.get(i).getId())) return i;
        }

        return -1;
    }

    private void readNewMessages() {
        resetUnseen();

        for (int i = 0; i < newMessages.size(); i++) {
            String id = newMessages.pop();
            DocumentReference messageRef = firestore
                    .document(CHATS + '/' + chatId + '/' + MESSAGES + '/' + id);
            messageRef.update(SEEN, true);
            DocumentReference notificationRef =
                    firestore.document(DEVICES + '/' +
                            application.getTokenId() + '/' + NOTIFICATIONS + '/' + id);
            notificationRef.delete();
        }
    }

    @Override
    protected void onResume() {
        application.setCurrentActivity(this);
        application.setAppActive(true);
        isActivityActive = true;
        super.onResume();
        readNewMessages();
        application.updateUserStatus(ONLINE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.setCurrentActivity(null);
        application.setAppActive(false);
        isActivityActive = false;
        application.disconnectUserDelayed();
    }

    public String getTargetUid() {
        return targetUid;
    }

    public void setLoggedUser(User loggedUser) {
        this.loggedUser = loggedUser;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
