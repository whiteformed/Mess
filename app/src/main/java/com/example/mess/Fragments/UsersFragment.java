package com.example.mess.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.mess.Adapter.UserAdapter;
import com.example.mess.MainActivity;
import com.example.mess.Model.User;
import com.example.mess.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.mess.Mess.USERNAME;
import static com.example.mess.Mess.USERS;

public class UsersFragment extends Fragment {

    private MainActivity mainActivity;

    private TextView txtNoMatch;
    private ProgressBar progressBar;

    private UserAdapter userAdapter;
    private ArrayList<User> users;

    private ArrayList<DocumentChange> pendingChanges;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_users, container, false);

        Context context = getContext();
        mainActivity = (MainActivity) getActivity();
        assert mainActivity != null;

        txtNoMatch = parentView.findViewById(R.id.txtNoMatch);
        RecyclerView recyclerView = parentView.findViewById(R.id.recyclerView);
        progressBar = parentView.findViewById(R.id.progressBar);

        DefaultItemAnimator animator = ((DefaultItemAnimator) recyclerView.getItemAnimator());
        assert animator != null;
        animator.setSupportsChangeAnimations(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        users = new ArrayList<>();
        userAdapter = new UserAdapter(context, this, users);
        recyclerView.setAdapter(userAdapter);

        pendingChanges = new ArrayList<>();

        readUsers();

        return parentView;
    }

    private void readUsers() {
        CollectionReference allUsers = FirebaseFirestore.getInstance().collection(USERS);

        Query orderedUsers = allUsers.orderBy(USERNAME, Query.Direction.ASCENDING);
        orderedUsers.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null || queryDocumentSnapshots == null) return;

            progressBar.setVisibility(View.GONE);

            handleDocumentChanges(queryDocumentSnapshots.getDocumentChanges(), false);
        });
    }

    private int addNewUser(User newUser) {
        for (int i = users.size() - 1; i >= 0; i--) {
            String username = newUser.getUsername();
            String tempUsername = users.get(i).getUsername();
            if (username.compareTo(tempUsername) > 0) {
                users.add(i + 1, newUser);
                return i + 1;
            }
        }

        users.add(0, newUser);
        return 0;
    }

    private int getUserIndex(String userId) {
        for (int i = 0; i < users.size(); i++) {
            if (userId.equals(users.get(i).getUid())) return i;
        }

        return -1;
    }

    public void onQueryTextChange(String newText) {
        if (userAdapter != null)
            userAdapter.getFilter().filter(newText);
    }

    public void toggleNoMatchesText(boolean visible) {
        if (txtNoMatch != null)
            txtNoMatch.setVisibility(visible ? View.VISIBLE : View.GONE);
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

            if (userAdapter.isFilterApplied()) {
                pendingChanges.add(documentChange);
                continue;
            }

            DocumentSnapshot document = documentChange.getDocument();
            User user = document.toObject(User.class);
            String userUid = document.getId();
            user.setUid(userUid);

            String photoThumbUrl = user.getPhotoThumbUrl();
            mainActivity.getChatsFragment().updateChatThumb(photoThumbUrl, userUid);

            switch (documentChange.getType()) {
                case ADDED:
                    if (!userUid.equals(loggedUid)) {
                        int newIndex = addNewUser(user);
                        userAdapter.itemInserted(newIndex, user);
                        userAdapter.notifyItemInserted(newIndex);
                    }

                    break;
                case MODIFIED:
                    int index = getUserIndex(userUid);

                    if (index != -1) {
                        users.set(index, user);
                        userAdapter.itemChanged(index, user);
                        userAdapter.notifyItemChanged(index);
                    }

                    break;
            }
        }
    }
}
