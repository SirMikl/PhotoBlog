package com.example.photoblog;

import java.util.Date;

public class Comments {
    private String message, user_id;
    private Date timesamp;

    public Comments(){}

    public Comments(String message, String user_id, Date timesamp) {
        this.message = message;
        this.user_id = user_id;
        this.timesamp = timesamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Date getTimesamp() {
        return timesamp;
    }

    public void setTimesamp(Date timesamp) {
        this.timesamp = timesamp;
    }
}
