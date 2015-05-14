package com.bitsplease.calcumessenger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.bitsplease.encryption.EncryptionUtil;
import com.bitsplease.utilities.Message;
import com.bitsplease.utilities.MessageBundle;
import com.bitsplease.utilities.MessageListingAdapter;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {

    private MessageListingAdapter adapter;
    private List<Message> listMessages;
    private ListView listViewMessages;

   // TextView messages;
    EditText enterMessage;
    Button sendMessage;

    private String senderName, receiverName;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String message = "";
    private String chatServerIP = "192.168.43.1";
    private int chatServerPort = 12344;
    private Socket client;
    private PublicKey senderPublicKey;
    SecretKey secretKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //messages = (TextView) findViewById(R.id.messages);
        enterMessage = (EditText) findViewById(R.id.message);
        sendMessage = (Button) findViewById(R.id.send_button);
        listViewMessages = (ListView) findViewById(R.id.list_view_messages);

        senderName = "You";
        receiverName = "You";

        listMessages = new ArrayList<Message>();

        adapter = new MessageListingAdapter(this, listMessages);
        listViewMessages.setAdapter(adapter);

        /*
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                startActivity(new Intent("android.credentials.UNLOCK"));
                Log.w("YES", "UNLOCKED!");
            } else {
                startActivity(new Intent("com.android.credentials.UNLOCK"));
                Log.w("YES", "UNLOCKED!");
            }
        } catch (ActivityNotFoundException e) {
            Log.e("YES", "No UNLOCK activity: " + e.getMessage(), e);
        }
        */

        //KEY GENERATION
        try{
            if (!EncryptionUtil.areKeysPresent()) {
                EncryptionUtil.generateKey(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        try {
            String ans = EncryptionUtil.signData(test);
            boolean right = EncryptionUtil.verifyData(test, ans);
            Log.w("YES", "*_*"+right);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        */

        Thread cThread = new Thread(new ClientThread());
        cThread.start();

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enterMessage.getText().toString() != ""){
                    sendData( enterMessage.getText().toString() );
                    displayMessage( new Message(enterMessage.getText().toString(), receiverName, true) );
                    enterMessage.setText("");
                }
            }
        });

    }

    public class ClientThread implements Runnable {

        public void run() {
            try
            {
                connectToServer();
                getStreams();
                senderPublicKey = handshake(); //send public key
                Log.w("YES", "HANDSHAKED!");
                processConnection();
            }
            catch ( EOFException eofException )
            {
               showToast( "Client terminated connection" );
            }
            catch ( IOException ioException )
            {
                ioException.printStackTrace();
            }
            catch (Exception e){
                //Log.w("YES", e.toString());
            }
            finally
            {
                //closeConnection();
            }
        }
    }

    private void connectToServer() throws IOException
    {
        showToast( "Attempting connection" );

        try{
            client = new Socket( chatServerIP, chatServerPort );
        } catch (Exception e){
            Log.w("YES", "*" + e.toString());
        }
        Log.w("YES", "CONNETED!");
        senderName = client.getInetAddress().getHostName();
        showToast( "Connected to: " + senderName );
    }

    private void getStreams() throws IOException {
        output = new ObjectOutputStream(client.getOutputStream());
        output.flush();
        input = new ObjectInputStream(client.getInputStream());
        showToast("Got I/O streams");
    }

    private PublicKey handshake() {
        //SEND MY PUBLIC KEY
        try {
            PublicKey pk = EncryptionUtil.getPublicKey();
            byte[] pKbytes = Base64.encode(pk.getEncoded(), 0);
            String pK = new String(pKbytes);
            String pubKey = "-----BEGIN PUBLIC KEY-----\n" + pK + "-----END PUBLIC KEY-----";

            Log.w("YES", "MY PUBLIC KEY = " + pubKey);

            output.writeObject(pubKey);
            output.flush();
        } catch (Exception e) {
            Log.w("YES", e.getMessage());
        }

        //WAIT FOR HIS PUBLIC KEY
        try {
            String senderPublicKeyString = ((String) input.readObject());
            if (senderPublicKeyString.startsWith("-----BEGIN PUBLIC KEY-----")) {
                // Remove the first and last lines
                senderPublicKeyString = senderPublicKeyString.replace("-----BEGIN PUBLIC KEY-----\n", "");
                senderPublicKeyString = senderPublicKeyString.replace("-----END PUBLIC KEY-----", "");

                byte[] keyBytes = Base64.decode(senderPublicKeyString.getBytes("utf-8"), 0);
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey key = keyFactory.generatePublic(spec);

                byte[] pKbytes = Base64.encode(key.getEncoded(), 0);
                String pK = new String(pKbytes);
                String pubKey = "-----BEGIN PUBLIC KEY-----\n" + pK + "-----END PUBLIC KEY-----";

                Log.w("YES", "HIS PUBLIC KEY = " + pubKey);

                if (key != null) {

                    MessageBundle messageBundle = ((MessageBundle) input.readObject());
                    //if (senderGeneratedAES.startsWith("-----BEGIN AES KEY-----")) {

                    Log.w("YES", "1- PLAIN TEXT = " + messageBundle.getPlainText());
                    Log.w("YES", "1- SIGNED TEXT = " + messageBundle.getSignedText());

                    boolean isVerified = EncryptionUtil.verifyData(messageBundle.getPlainText(), messageBundle.getSignedText(), key);

                    Log.w("YES", "*_*"+ messageBundle.getPlainText());

                    if (isVerified) {
                        String receivedAESKey = EncryptionUtil.decryptPrivateKey(messageBundle.getPlainText());
                        receivedAESKey = receivedAESKey.substring(receivedAESKey.indexOf("-----BEGIN AES KEY-----"));

                        Log.w("YES", "OOH = "+ receivedAESKey);
                        //displayMessage(new Message(messageBundle.getPlainText(), senderName, false));

                        receivedAESKey = receivedAESKey.replace("-----BEGIN AES KEY-----\n", "");
                        receivedAESKey = receivedAESKey.replace("-----END AES KEY-----", "");
                        byte[] secretKeyBytes = Base64.decode(receivedAESKey.getBytes("utf-8"), 0);
                        secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");
                        Log.w("YES", "THE RECEIVED AES SECRET KEY = " + new String(receivedAESKey));
                        return key;
                    }
                }
                //}
            }

        } catch (ClassNotFoundException classNotFoundException) {
            showToast("Unknown object type received");
            Log.w("YES", classNotFoundException.getMessage());
        } catch (Exception e) {
            Log.w("YES", e.getMessage());
        }
        return null;
    }


    private void processConnection() throws IOException
    {
        Log.w("YES", "PROCSSING!!");
        MessageBundle messageBundle = null;
        do
        {
            try
            {
                //get the object that contains the plain text and signed text
                messageBundle = ( MessageBundle ) input.readObject();

                //decrypt its contents using my private key
                messageBundle.setPlainText(EncryptionUtil.decryptAES(messageBundle.getPlainText(), secretKey));
                messageBundle.setSignedText(EncryptionUtil.decryptAES(messageBundle.getSignedText(), secretKey));

                //decrypt using his public key and compare hashes
                Log.w("YES", "1- PLAIN TEXT = " + messageBundle.getPlainText());
                Log.w("YES", "1- SIGNED TEXT = " + messageBundle.getSignedText());

                boolean isVerified = EncryptionUtil.verifyData(messageBundle.getPlainText(), messageBundle.getSignedText(), senderPublicKey);

                if (isVerified){
                    displayMessage(new Message(messageBundle.getPlainText(), senderName, false));
                }

            }
            catch ( ClassNotFoundException classNotFoundException ) {
                Log.w("YES", "ERROR 1");
                showToast("Unknown object type received");
            } catch (Exception e){
                // Log.w("YES", "Foo didn't work: " + e.getMessage());
            }

        } while (true);
    }

    private void closeConnection()
    {
        showToast( "Closing connection" );
        try
        {
            output.close();
            input.close();
            client.close();
        }
        catch ( IOException ioException )
        {
            ioException.printStackTrace();
        }
    }

    private void sendData( String message )
    {
        try
        {
            //Encrypt the string using my private key
            String signedText = EncryptionUtil.signDataPrivateKey(message);
            //Log.w("YES", "SIGNED TEXT = " + signedText);
            //pack the plain and signed messages
            MessageBundle messageBundle = new MessageBundle(message, signedText);
            Log.w("YES", "1- PLAIN TEXT = " + messageBundle.getPlainText());
            Log.w("YES", "1- SIGNED TEXT = " + messageBundle.getSignedText());

            //encrypt object contents using symmetric key
            messageBundle.setPlainText(EncryptionUtil.encryptAES(messageBundle.getPlainText(), secretKey));
            messageBundle.setSignedText(EncryptionUtil.encryptAES(messageBundle.getSignedText(), secretKey));

            Log.w("YES", "2- PLAIN TEXT = " + messageBundle.getPlainText());
            Log.w("YES", "2- SIGNED TEXT = " + messageBundle.getSignedText());

            output.writeObject(messageBundle);
            output.flush();
        }
        catch ( IOException ioException )
        {
          showToast("Error writing object");
          Log.w("YES", "ERROR 1 " + ioException.toString());
        }
        catch (Exception e) {
          showToast("Error writing object");
          Log.w("YES", "ERROR 2 " + e.toString());
        }
    }


    private void displayMessage( final Message messageToDisplay )
    {
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                listMessages.add(messageToDisplay);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showToast(final String message) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

}
