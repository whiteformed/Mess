package com.example.mess.Fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.example.mess.Adapter.ChatAdapter;
import com.example.mess.MainActivity;
import com.example.mess.Model.Chat;
import com.example.mess.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.mess.Mess.CHATS;
import static com.example.mess.Mess.DATE;
import static com.example.mess.Mess.FIRST;
import static com.example.mess.Mess.FIRST_UNSEEN;
import static com.example.mess.Mess.FIRST_USERNAME;
import static com.example.mess.Mess.LAST_MESSAGE;
import static com.example.mess.Mess.LAST_MESSAGE_FROM;
import static com.example.mess.Mess.SECOND;
import static com.example.mess.Mess.SECOND_UNSEEN;
import static com.example.mess.Mess.SECOND_USERNAME;

public class ChatsFragment extends Fragment {

    private Context context;
    private MainActivity mainActivity;

    private ProgressBar progressBar;

    private ChatAdapter chatAdapter;
    private List<Chat> chatList;

    private FirebaseFirestore firestore;

    private ArrayList<DocumentChange> pendingChanges;

    private ArrayList<String> pendingPhotoThumbs;
    private ArrayList<String> pendingUserIds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_chats, container, false);

        context = getContext();
        mainActivity = (MainActivity) getActivity();

        firestore = FirebaseFirestore.getInstance();

        progressBar = parentView.findViewById(R.id.progressBar);
        RecyclerView recyclerView = parentView.findViewById(R.id.recyclerView);

        DefaultItemAnimator animator = ((DefaultItemAnimator) recyclerView.getItemAnimator());
        assert animator != null;
        animator.setSupportsChangeAnimations(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(context, chatList);
        recyclerView.setAdapter(chatAdapter);

        pendingChanges = new ArrayList<>();
        pendingPhotoThumbs = new ArrayList<>();
        pendingUserIds = new ArrayList<>();

        readChats();

        return parentView;
    }

    private void readChats() {
        CollectionReference allChats = firestore.collection(CHATS);

        Query orderedChats = allChats.orderBy(DATE, Query.Direction.ASCENDING);
        orderedChats.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null || queryDocumentSnapshots == null) return;

            progressBar.setVisibility(View.GONE);
            handleDocumentChanges(queryDocumentSnapshots.getDocumentChanges(), false);
        });
    }

    private int getChatIndex(String userId) {
        for (int i = 0; i < chatList.size(); i++) {
            if (userId.equals(chatList.get(i).getTargetUid())) return i;
        }

        return -1;
    }

    public void handleChanges() {
        handleDocumentChanges(pendingChanges, true);
    }

    private void handleDocumentChanges(List<DocumentChange> documentChanges, boolean calledOnResume) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || documentChanges == null) return;
        String loggedUid = firebaseUser.getUid();
        int size = documentChanges.size();
        for (int i = 0; i < size; i++) {
            DocumentChange documentChange = calledOnResume ?
                    documentChanges.remove(0) : documentChanges.get(i);

            if (mainActivity.isActivityNotActive()) {
                if (calledOnResume)
                    break;
                else {
                    pendingChanges.add(documentChange);
                    continue;
                }
            }

            DocumentSnapshot document = documentChange.getDocument();

            String chatId = document.getId();
            if (!chatId.contains(loggedUid)) continue;

            String[] UIDs = chatId.split("-");

            String targetUsername, targetUid;
            int unseen;
            String lastMessage = (String) document.get(LAST_MESSAGE);
            String lastMessageFrom = (String) document.get(LAST_MESSAGE_FROM);
            assert lastMessageFrom != null;
            Timestamp timestamp = (Timestamp) document.get(DATE);
            Date date = timestamp == null ? null : timestamp.toDate();
            boolean lastMessageFromUser;
            if (loggedUid.equals(UIDs[0])) {
                targetUsername = (String) document.get(SECOND_USERNAME);
                targetUid = UIDs[1];
                unseen = Long.valueOf((long) document.get(FIRST_UNSEEN)).intValue();
                lastMessageFromUser = lastMessageFrom.equals(FIRST);
            } else {
                targetUsername = (String) document.get(FIRST_USERNAME);
                targetUid = UIDs[0];
                unseen = Long.valueOf((long) document.get(SECOND_UNSEEN)).intValue();
                lastMessageFromUser = lastMessageFrom.equals(SECOND);
            }

            switch (documentChange.getType()) {
                case ADDED:
                    chatList.add(0, new Chat(targetUid, targetUsername, lastMessage, date, unseen, lastMessageFromUser));
                    chatAdapter.notifyItemInserted(0);
                    updatePendingThumb(targetUid);
                    break;
                case MODIFIED:
                    int index = getChatIndex(targetUid);

                    if (index != -1) {
                        Chat chat = chatList.get(index);
                        Date oldDate = chat.getDate();
                        chat.setTargetUsername(targetUsername);
                        chat.setLastMessage(lastMessage);
                        chat.setLastMessageFromUser(lastMessageFromUser);
                        chat.setDate(date);
                        chat.setUnseen(unseen);
                        if (oldDate != null && !oldDate.equals(date)) {
                            chatList.remove(index);
                            chatList.add(0, chat);
                            chatAdapter.notifyItemMoved(index, 0);
                            chatAdapter.notifyItemChanged(0);
                        } else {
                            chatAdapter.notifyItemChanged(index);
                        }
                    }
                    break;
            }
        }
    }

    private void updatePendingThumb(String userId) {
        for (int i = 0; i < pendingUserIds.size(); i++) {
            if (pendingUserIds.get(i).equals(userId)) {
                String pendingThumbUrl = pendingPhotoThumbs.remove(0);
                String pendingUserId = pendingUserIds.remove(0);
                updateChatThumb(pendingThumbUrl, pendingUserId);
            }
        }
    }

    void updateChatThumb(String photoThumbUrl, String userId) {
        int chatIndex = getChatIndex(userId);
        if (chatIndex == -1) {
            pendingPhotoThumbs.add(photoThumbUrl);
            pendingUserIds.add(userId);
            return;
        }

        Chat chat = chatList.get(chatIndex);
        chat.setPhotoThumbUrl(photoThumbUrl);
        chatAdapter.notifyItemChanged(chatIndex);
    }

    @Override
    public void onResume() {
        super.onResume();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancelAll();
    }
}
