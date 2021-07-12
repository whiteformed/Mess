package com.example.mess.Model;

import java.util.Date;

@SuppressWarnings("unused")
public class User {
    private String uid;
    private String tokenId;
    private String username;
    private String email;
    private String status;
    private String photoUrl;
    private String photoThumbUrl;
    private Date lastSeen;

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPhotoThumbUrl() {
        return photoThumbUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setPhotoThumbUrl(String photoThumbUrl) {
        this.photoThumbUrl = photoThumbUrl;
    }

    public Date getLastSeen() {
        return lastSeen;
    }
}
