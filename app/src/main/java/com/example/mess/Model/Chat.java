package com.example.mess.Model;

import java.util.Date;

public class Chat {
    private String photoThumbUrl;
    private String targetUsername;
    private String targetUid;
    private String lastMessage;
    private int unseen;
    private Date date;
    private boolean lastMessageFromUser;

    public Chat(String targetUid, String targetUsername, String lastMessage, Date date,
                int unseen, boolean lastMessageFromUser) {
        this.targetUid = targetUid;
        this.targetUsername = targetUsername;
        this.lastMessage = lastMessage;
        this.date = date;
        this.unseen = unseen;
        this.lastMessageFromUser = lastMessageFromUser;
    }

    public void setUnseen(int unseen) {
        this.unseen = unseen;
    }

    public int getUnseen() {
        return unseen;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getTargetUid() {
        return targetUid;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setLastMessageFromUser(boolean lastMessageFromUser) {
        this.lastMessageFromUser = lastMessageFromUser;
    }

    public boolean isLastMessageFromUser() {
        return lastMessageFromUser;
    }

    public String getPhotoThumbUrl() {
        return photoThumbUrl;
    }

    public void setPhotoThumbUrl(String photoThumbUrl) {
        this.photoThumbUrl = photoThumbUrl;
    }
}
