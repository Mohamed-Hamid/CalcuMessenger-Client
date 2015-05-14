package com.bitsplease.utilities;


public class Message {
    private String message, senderName;
    private boolean isSelf;

    public Message(String message, String senderName, boolean isSelf) {
        this.message = message;
        this.isSelf = isSelf;
        this.senderName = senderName;
    }

    public Message(String fromName) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public String getSenderName(){
        return this.senderName;
    }

    public void setSenderName(String SenderName){
        this.senderName = SenderName;
    }

}