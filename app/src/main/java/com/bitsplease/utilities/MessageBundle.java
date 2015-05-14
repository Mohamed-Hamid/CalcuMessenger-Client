package com.bitsplease.utilities;

import java.io.Serializable;

/**
 * Created by Mohamed on 5/12/2015.
 */
public class MessageBundle implements Serializable {

    String plainText, signedText;

    public MessageBundle(String plainText, String signedText){
        this.plainText = plainText;
        this.signedText = signedText;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public String getSignedText() {
        return signedText;
    }

    public void setSignedText(String signedText) {
        this.signedText = signedText;
    }

}
