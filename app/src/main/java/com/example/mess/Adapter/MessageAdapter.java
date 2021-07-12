package com.example.mess.Adapter;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mess.ChatActivity;
import com.example.mess.Model.ChatMessage;
import com.example.mess.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.List;

import static com.example.mess.Mess.TAG_PICASSO;
import static com.example.mess.Mess.TYPE_PHOTO;
import static com.example.mess.Mess.TYPE_TEXT;

public class MessageAdapter extends RecyclerView.Adapter {
    private static final int MSG_TEXT_SENT = 0;
    private static final int MSG_TEXT_RECEIVED = 1;
    private static final int MSG_IMAGE_SENT = 2;
    private static final int MSG_IMAGE_RECEIVED = 3;

    private int lastDelivered = -1, lastSeen = -1, pendingImageLoads;

    private Context context;
    private List<ChatMessage> messages;

    private MessageHolder lastDeliveredHolder, lastSeenHolder;

    public MessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (viewType) {
            default:
            case MSG_TEXT_SENT:
                view = inflater.inflate(R.layout.message_text_right, parent, false);
                return new MessageHolder(view, true);
            case MSG_TEXT_RECEIVED:
                view = inflater.inflate(R.layout.message_text_left, parent, false);
                return new MessageHolder(view, false);
            case MSG_IMAGE_SENT:
                view = inflater.inflate(R.layout.message_image_right, parent, false);
                return new ImageMessageHolder(view, true);
            case MSG_IMAGE_RECEIVED:
                view = inflater.inflate(R.layout.message_image_left, parent, false);
                return new ImageMessageHolder(view, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = messages.get(position);
        MessageHolder h = (MessageHolder) holder;
        h.txtMessage.setText(chatMessage.getMessage());

        if (h instanceof ImageMessageHolder) {
            ImageMessageHolder ih = (ImageMessageHolder) h;
            ih.imageProgressBar.setVisibility(View.VISIBLE);
            String photoUrl = chatMessage.getPhotoUrl();
            ih.txtMessage.setVisibility(View.GONE);
            ih.imgMessage.setVisibility(View.GONE);
            ih.photoUrl = null;

            loadPhoto(photoUrl, ih, position, 0, false);
        }

        switch (getItemViewType(position)) {
            case MSG_IMAGE_SENT:
            case MSG_TEXT_SENT:
                h.txtStatus.setVisibility(View.GONE);

                if (chatMessage.isSeen()) {
                    if (position >= lastSeen) {
                        lastSeen = position;

                        if (lastSeenHolder != null)
                            lastSeenHolder.txtStatus.setVisibility(View.GONE);
                        lastSeenHolder = h;
                        h.txtStatus.setVisibility(View.VISIBLE);
                        h.txtStatus.setText(R.string.seen);
                    }
                } else if (chatMessage.isDelivered()) {
                    if (position >= lastDelivered) {
                        lastDelivered = position;

                        if (lastDeliveredHolder != null && lastDeliveredHolder != lastSeenHolder)
                            lastDeliveredHolder.txtStatus.setVisibility(View.GONE);
                        lastDeliveredHolder = h;
                        h.txtStatus.setVisibility(View.VISIBLE);
                        h.txtStatus.setText(R.string.delivered);
                    }
                }
                break;
        }
    }

    private void loadPhoto(String photoUrl, ImageMessageHolder h, int position, int attempt, boolean forceOnline) {
        if (photoUrl == null) return;

        RequestCreator requestCreator = Picasso.get().load(photoUrl);

        if (!forceOnline && h.photoUrl != null)
            requestCreator = requestCreator.networkPolicy(NetworkPolicy.OFFLINE);

        ChatActivity chatActivity = (ChatActivity) context;
        RecyclerView recyclerView = chatActivity.getRecyclerView();

        if (h.photoUrl == null) {
            if (position == messages.size() - 1 && pendingImageLoads == -1)
                pendingImageLoads = 1;
            else if (pendingImageLoads != -1) {
                pendingImageLoads++;
            }
        }

        requestCreator
                .resize(800, 800)
                .centerInside()
                .onlyScaleDown()
                .into(h.imgMessage, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (h.photoUrl == null && pendingImageLoads != -1)
                            pendingImageLoads--;
                        h.imgMessage.setVisibility(View.VISIBLE);
                        h.imageProgressBar.setVisibility(View.GONE);
                        if (h.photoUrl == null && pendingImageLoads == 0) {
                            Handler handler = new Handler();
                            handler.postDelayed(() -> {
                                recyclerView.smoothScrollToPosition(messages.size() - 1);
                                pendingImageLoads = -1;
                            }, 100);
                        }
                        h.photoUrl = photoUrl;
                    }

                    @Override
                    public void onError(Exception e) {
                        if (h.photoUrl == null && pendingImageLoads != -1)
                            pendingImageLoads--;
                        if (attempt < 3)
                            loadPhoto(photoUrl, h, position, attempt + 1, true);
                        else {
                            h.imgMessage.setVisibility(View.VISIBLE);
                            h.imgMessage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_image));
                            Log.e(TAG_PICASSO, e.getMessage(), e);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage chatMessage = messages.get(position);
        String messageType = chatMessage.getType();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;

        if (messageType == null) messageType = TYPE_TEXT;

        if (chatMessage.getSender().equals(firebaseUser.getUid())) {
            switch (messageType) {
                default:
                case TYPE_TEXT:
                    return MSG_TEXT_SENT;
                case TYPE_PHOTO:
                    return MSG_IMAGE_SENT;
            }
        } else {
            switch (messageType) {
                default:
                case TYPE_TEXT:
                    return MSG_TEXT_RECEIVED;
                case TYPE_PHOTO:
                    return MSG_IMAGE_RECEIVED;
            }
        }
    }

    static class MessageHolder extends RecyclerView.ViewHolder {
        TextView txtMessage;
        TextView txtStatus;

        MessageHolder(@NonNull View itemView, boolean typeSent) {
            super(itemView);

            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtStatus = typeSent ? itemView.findViewById(R.id.txtStatus) : null;
        }
    }

    static class ImageMessageHolder extends MessageHolder {
        String photoUrl;

        ImageView imgMessage;
        ProgressBar imageProgressBar;

        ImageMessageHolder(@NonNull View itemView, boolean typeSent) {
            super(itemView, typeSent);

            imgMessage = itemView.findViewById(R.id.imgMessage);
            imageProgressBar = itemView.findViewById(R.id.imageProgressBar);
        }
    }

    public void setPendingImageLoads(int pendingImageLoads) {
        this.pendingImageLoads = pendingImageLoads;
    }
}
