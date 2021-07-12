package com.example.mess.Adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mess.ChatActivity;
import com.example.mess.Fragments.UsersFragment;
import com.example.mess.Mess;
import com.example.mess.MainActivity;
import com.example.mess.Model.User;
import com.example.mess.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.mess.Mess.FADE_DURATION;
import static com.example.mess.Mess.PHOTO_THUMB_URL;
import static com.example.mess.Mess.TAG_PICASSO;
import static com.example.mess.Mess.UID;
import static com.example.mess.Mess.USERNAME;

public class UserAdapter extends RecyclerView.Adapter implements Filterable {
    private UsersFragment usersFragment;
    private Context context;
    private ArrayList<User> users;
    private ArrayList<User> allUsers;

    private boolean isFilterApplied;

    public UserAdapter(Context context, UsersFragment usersFragment, ArrayList<User> users) {
        this.context = context;
        this.usersFragment = usersFragment;
        allUsers = users;
        this.users = new ArrayList<>(users);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UserViewHolder h = (UserViewHolder) holder;
        User user = users.get(position);
        h.txtUsername.setText(user.getUsername());
        h.txtEmail.setText(user.getEmail());

        String photoThumbUrl = user.getPhotoThumbUrl();

        h.imgProfileHolder.setColorFilter(ContextCompat.getColor(context, R.color.colorFg));
        h.imgProfileHolder.setAlpha((float) 1);
        h.imgProfileThumb.setAlpha((float) 0);

        if (photoThumbUrl != null) {
            boolean isPhotoChanged = !photoThumbUrl.equals(h.photoThumbUrl);

            RequestCreator requestCreator = Picasso.get().load(photoThumbUrl);

            if (!isPhotoChanged)
                requestCreator = requestCreator.networkPolicy(NetworkPolicy.OFFLINE);

            requestCreator.into(h.imgProfileThumb, new Callback() {
                @Override
                public void onSuccess() {
                    if (isPhotoChanged && !isFilterApplied)
                        h.imgProfileThumb.animate().alpha(1).setDuration(FADE_DURATION);
                    else
                        h.imgProfileThumb.setAlpha((float) 1);
                    h.imgProfileHolder.animate().alpha(0).setDuration(FADE_DURATION);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG_PICASSO, e.getMessage(), e);
                }
            });
        }

        h.photoThumbUrl = photoThumbUrl;

        h.itemView.setOnClickListener(v -> {
            Mess application = (Mess) context.getApplicationContext();
            if (application.getLoggedUser() != null) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(UID, user.getUid());
                intent.putExtra(USERNAME, user.getUsername());
                intent.putExtra(PHOTO_THUMB_URL, photoThumbUrl);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        String photoThumbUrl;

        ImageView imgProfileThumb, imgProfileHolder;
        TextView txtUsername, txtEmail;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfileThumb = itemView.findViewById(R.id.imgProfileThumb);
            imgProfileHolder = itemView.findViewById(R.id.imgProfileHolder);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtEmail = itemView.findViewById(R.id.txtEmail);
        }
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        private boolean isFiltered;

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<User> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(allUsers);
                isFiltered = false;
                usersFragment.handleChanges();
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (User user : allUsers) {
                    if (user.getUsername().toLowerCase().contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }
                isFiltered = true;
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            users.clear();
            List<User> filteredUsers = (List) results.values;
            users.addAll(filteredUsers);
            notifyDataSetChanged();

            isFilterApplied = isFiltered;

            MainActivity mainActivity = (MainActivity) context;
            mainActivity.toggleNoUserMatchesText(filteredUsers.size() == 0);
        }
    };

    public void itemInserted(int index, User newUser) {
        users.add(index, newUser);
    }

    public void itemChanged(int index, User newUser) {
        users.set(index, newUser);
    }

    public boolean isFilterApplied() {
        return isFilterApplied;
    }
}
