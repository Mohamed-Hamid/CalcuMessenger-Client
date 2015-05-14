package com.bitsplease.encryption;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import android.util.Log;

import com.bitsplease.calcumessenger.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

public class EncryptionUtil {

    public static final String ALGORITHM = "RSA";
    private static String mAlias = "myKey"; //to retreive keys b3ad kda
    public static final String KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore";

    public static final String SIGNATURE_SHA256withRSA = "SHA256withRSA";
    public static final String SIGNATURE_SHA512withRSA = "SHA512withRSA";

    public static void generateKey(Context context) {
        try {

            Calendar start = new GregorianCalendar();
            Calendar end = new GregorianCalendar();
            end.add(Calendar.YEAR, Calendar.YEAR);

            KeyPairGeneratorSpec spec =
                    new KeyPairGeneratorSpec.Builder(context)
                            // You'll use the alias later to retrieve the key.  It's a key for the key!
                            .setAlias(mAlias)
                                    // The subject used for the self-signed certificate of the generated pair
                            .setSubject(new X500Principal("CN=" + mAlias))
                                    // The serial number used for the self-signed certificate of the
                                    // generated pair.
                            .setSerialNumber(BigInteger.valueOf(1337))
                                    // Date range of validity for the generated pair.
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .setKeySize(1024)
                            .build();

            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER_ANDROID_KEYSTORE);

            keyGen.initialize(spec);
            final KeyPair key = keyGen.generateKeyPair();

            //key.getPrivate();
            //key.getPublic();

            Log.d("YES", "Public Key is: " + key.getPublic().toString());
            Log.d("YES", "Private Key is: " + key.getPrivate().toString());

        } catch (Exception e) {
            Log.w("YES", "**" + e.toString());
            e.printStackTrace();
        }

    }

    public static boolean areKeysPresent() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");

        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null);

        // Load the key pair from the Android Key Store
        KeyStore.Entry entry = ks.getEntry(mAlias, null);

        if (entry == null) {
            Log.w("YES", "No key found under alias: " + mAlias);
            Log.w("YES", "Exiting verifyData()...");
            return false;
        } else {
            Log.w("YES", "key found under alias: " + mAlias);
            return true;
        }
    }

    public static String signDataPrivateKey(String inputStr) throws KeyStoreException,
            UnrecoverableEntryException, NoSuchAlgorithmException, InvalidKeyException,
            SignatureException, IOException, CertificateException {
        byte[] data = inputStr.getBytes();

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);

        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(mAlias, null);

        if (entry == null) {
            Log.w("YES", "No key found under alias: " + mAlias);
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w("YES", "Not an instance of a PrivateKeyEntry");
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        Signature s = Signature.getInstance(SIGNATURE_SHA256withRSA);
        s.initSign(((KeyStore.PrivateKeyEntry) entry).getPrivateKey());
        s.update(data);
        byte[] signature = s.sign();
        String result = Base64.encodeToString(signature, Base64.DEFAULT);
        return result;
    }

    public static boolean verifyData(String input, String signatureStr) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableEntryException, InvalidKeyException, SignatureException {
        byte[] data = input.getBytes();
        byte[] signature;
        if (signatureStr == null) {
            Log.w("YES", "Invalid signature.");
            Log.w("YES", "Exiting verifyData()...");
            return false;
        }
        try {
            signature = Base64.decode(signatureStr, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return false;
        }

        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");

        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(mAlias, null);

        if (entry == null) {
            Log.w("YES", "No key found under alias: " + mAlias);
            Log.w("YES", "Exiting verifyData()...");
            return false;
        }

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w("YES", "Not an instance of a PrivateKeyEntry");
            return false;
        }

        Signature s = Signature.getInstance(SIGNATURE_SHA256withRSA);

        s.initVerify(((KeyStore.PrivateKeyEntry) entry).getCertificate());
        s.update(data);

        boolean valid = s.verify(signature);
        return valid;
    }


    public static String encryptDataPublicKey(String text, PublicKey key) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] cipherText = null;
        final Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipherText = cipher.doFinal(text.getBytes());
        String encryptedText= new String(Base64.encode(cipherText, 0));
        return encryptedText;
    }

    public static SecretKey generateAES() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

    public static PrivateKey getPrivateKey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException {

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);

        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(mAlias, null);

        if (entry == null) {
            Log.w("YES", "No key found under alias: " + mAlias);
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w("YES", "Not an instance of a PrivateKeyEntry");
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }



    public static PublicKey getPublicKey() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException {

        KeyStore ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);

        ks.load(null);

        KeyStore.Entry entry = ks.getEntry(mAlias, null);

        if (entry == null) {
            Log.w("YES", "No key found under alias: " + mAlias);
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            Log.w("YES", "Not an instance of a PrivateKeyEntry");
            Log.w("YES", "Exiting signData()...");
            return null;
        }

        return ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
    }

    public static String decrypt(String message, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException {
        byte[] dectyptedText = null;
        byte[] decodedMessage = Base64.decode(message, 0);
        // get an RSA cipher object and print the provider
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding", "AndroidOpenSSL");
        // decrypt the text using the private key
        cipher.init(Cipher.DECRYPT_MODE, key);
        dectyptedText = cipher.doFinal(decodedMessage);
        return new String(dectyptedText);
    }

    public static String encryptAES(String dataToEncrypt, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher c = Cipher.getInstance("AES");
        //SecretKeySpec k = new SecretKeySpec(key, "AES");
        c.init(Cipher.ENCRYPT_MODE, key);
        String encryptedText= new String(Base64.encode(c.doFinal(dataToEncrypt.getBytes()), 0));
        return encryptedText;
    }

    public static String decryptAES(String message, SecretKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] dectyptedText = null;
        byte[] decodedMessage = Base64.decode(message, 0);

        Cipher c = Cipher.getInstance("AES");
        //SecretKeySpec k = new SecretKeySpec(key, "AES");
        c.init(Cipher.DECRYPT_MODE, key);
        dectyptedText = c.doFinal(decodedMessage);
        return new String(dectyptedText);
    }

}
