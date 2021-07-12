package com.example.mess.Adapter;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mess.ChatActivity;
import com.example.mess.Mess;
import com.example.mess.Model.Chat;
import com.example.mess.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.mess.Mess.FADE_DURATION;
import static com.example.mess.Mess.KEY_PHOTO_ICON;
import static com.example.mess.Mess.PHOTO_THUMB_URL;
import static com.example.mess.Mess.TAG_PICASSO;
import static com.example.mess.Mess.UID;
import static com.example.mess.Mess.USERNAME;

public class ChatAdapter extends RecyclerView.Adapter {
    private Context context;
    private List<Chat> chatList;

    public ChatAdapter(Context context, List<Chat> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatViewHolder h = (ChatViewHolder) holder;
        Chat chat = chatList.get(position);

        h.txtUsername.setText(chat.getTargetUsername());

        String lastMessage = chat.getLastMessage();
        if (lastMessage.equals(KEY_PHOTO_ICON)) {
            h.imgPhotoMessage.setVisibility(View.VISIBLE);
            lastMessage = context.getString(R.string.photo);
        } else {
            h.imgPhotoMessage.setVisibility(View.GONE);
        }

        h.txtLastMessage.setText(lastMessage);

        h.txtFromYou.setVisibility(chat.isLastMessageFromUser() ? View.VISIBLE : View.GONE);

        Date date = chat.getDate();
        if (date == null) date = Calendar.getInstance().getTime();

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

        h.txtDate.setText(strDate);

        int unseen = chat.getUnseen();
        if (unseen == 0) {
            h.txtUnseen.setVisibility(View.GONE);
        } else {
            String strUnseen = "" + unseen;
            h.txtUnseen.setVisibility(View.VISIBLE);
            h.txtUnseen.setText(strUnseen);
        }

        String photoThumbUrl = chat.getPhotoThumbUrl();

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
                    if (isPhotoChanged)
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
                intent.putExtra(UID, chat.getTargetUid());
                intent.putExtra(USERNAME, chat.getTargetUsername());
                intent.putExtra(PHOTO_THUMB_URL, chat.getPhotoThumbUrl());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfileThumb, imgProfileHolder, imgPhotoMessage;
        TextView txtUsername, txtLastMessage, txtDate, txtUnseen, txtFromYou;

        String photoThumbUrl;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            imgProfileThumb = itemView.findViewById(R.id.imgProfileThumb);
            imgProfileHolder = itemView.findViewById(R.id.imgProfileHolder);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtUnseen = itemView.findViewById(R.id.txtUnseen);
            txtFromYou = itemView.findViewById(R.id.txtFromYou);
            imgPhotoMessage = itemView.findViewById(R.id.imgPhotoMessage);
        }
    }
}
