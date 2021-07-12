package com.example.mess.Model;

import java.util.Date;

@SuppressWarnings("unused")
public class ChatMessage {
    private boolean seen, delivered;
    private String id, sender, receiver, message, type, photoUrl;
    private Date date;

    public ChatMessage() {
    }

    public ChatMessage(String id, boolean delivered, String sender, String receiver,
                       String message, String type, String photoUrl) {
        this.id = id;
        this.delivered = delivered;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.type = type;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public Date getDate() {
        return date;
    }

    public boolean isSeen() {
        return seen;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getType() {
        return type;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
