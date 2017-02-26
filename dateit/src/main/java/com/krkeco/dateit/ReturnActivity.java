package com.krkeco.dateit;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.InterstitialAd;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.krkeco.dateit.FireBase.LogInActivity;
import com.krkeco.dateit.admob.AdMob;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class ReturnActivity extends AppCompatActivity
        implements
        NfcAdapter.CreateNdefMessageCallback,
        NfcAdapter.OnNdefPushCompleteCallback{
    NfcAdapter mNfcAdapter;
    TextView mInfoText;
    private static final int MESSAGE_SENT = 1;

    LinearLayout main;
    TextView textView;
    private InterstitialAd mInterstitialAd;
    private AdMob adMob;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private String mUserId;

    String eventId = "newevent";//Long.toString(System.currentTimeMillis());
    String DB_ID = "dateit";
    /*
    {
      "rules": {
        "dateit": {
          "$uid": {
            ".read": "auth != null && auth.uid == $uid",
            ".write": "auth != null && auth.uid == $uid",
            "$event_id": {
              "item": {
                "title": {
                  ".validate": "newData.isString() && newData.val().length > 0"
                }
              }
            }
          }
        }
      }
    }
    rules bu for firebasedb

     */
    ArrayList<String> calendarList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        initAdmob();

        initLayout();

        initNFC();

        initFireBaseAuth();

        uploadIntentToFB();

    }

    public void uploadIntentToFB(){

        Intent intent = getIntent();
        if(intent.hasExtra("data")) {
            calendarList = getIntent().getStringArrayListExtra("data");

            Log.v("akrkeco",calendarList.toString());

            for(int x = 0; x < calendarList.size(); x++) {

                sendFBItem(calendarList.get(x));
             }
        }
    }

    public void sendFBItem(String string){//this is so all sends follow same rules in db
        com.krkeco.dateit.FireBase.Item item = new com.krkeco.dateit.FireBase.Item(string);
        mDatabase.child(DB_ID).child(mUserId).child(eventId).push().setValue(item);

    }

    private void loadLogInView() {
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void initFireBaseAuth(){

        // Initialize Firebase Auth and Database Reference
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mFirebaseUser == null) {
            // Not logged in, launch the Log In activity
            loadLogInView();
        } else {
            mUserId = mFirebaseUser.getUid();

            // Set up ListView
            final ListView listView = (ListView) findViewById(R.id.listView);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
            listView.setAdapter(adapter);

            // Use Firebase to populate the list.
            mDatabase.child(DB_ID).child(mUserId).child(eventId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    adapter.add((String) dataSnapshot.child("title").getValue());
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    adapter.remove((String) dataSnapshot.child("title").getValue());
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }

    public void initAdmob() {
        adMob = new AdMob(this);
        mInterstitialAd = adMob.newInterstitialAd();

        adMob.showInterstitial();
    }

    public void log(String string){
        Log.v("akrkeco",string);
    }

    public void initLayout(){

        setContentView(R.layout.activity_return);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading, please wait", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                adMob.showInterstitial();

            }
        });

        //get bundle/details and create separate textbox for each date with onclick to add to calendar
        main = (LinearLayout) findViewById(R.id.return_llayout);


    }

    public void initNFC(){

        mInfoText = (TextView) textView;// findViewById(R.id.intro_return_textview);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mInfoText = (TextView) textView;
            mInfoText.setText("NFC is not available on this device.");
        }
        // Register callback to set NDEF message
        mNfcAdapter.setNdefPushMessageCallback(this, this);
        // Register callback to listen for message-sent success
        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
    }


    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
       // Time time = new Time();
      //  time.setToNow();
        NdefMessage msg;
        if(calendarList != null){
            ByteArrayOutputStream calendarByte = new ByteArrayOutputStream();
            DataOutputStream calendarOStream = new DataOutputStream(calendarByte);
            for (String element : calendarList) {
                try {
                    calendarOStream.writeUTF(element);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            byte[] calendarByteArray = calendarByte.toByteArray();
            msg= new NdefMessage(
                    new NdefRecord[]{createMimeRecord(
                            "application/com.krkeco.dateit", calendarByteArray)
                            /**
                             * The Android Application Record (AAR) is commented out. When a device
                             * receives a push with an AAR in it, the application specified in the AAR
                             * is guaranteed to run. The AAR overrides the tag dispatch system.
                             * You can add it back in to guarantee that this
                             * activity starts when receiving a beamed message. For now, this code
                             * uses the tag dispatch system.
                            */
                            //,NdefRecord.createApplicationRecord("com.example.android.beam")
                    });
        }else {
            Snackbar.make(main, "Please setup your calendar settings by clicking the FAB before beaming content"
                    , Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            msg = null;
        }
           /* String text = "please click the calendar FAB and select your settings to send";
            msg = new NdefMessage(
                    new NdefRecord[]{createMimeRecord(
                            "application/com.krkeco.dateit", text.getBytes())
                            /**
                             * The Android Application Record (AAR) is commented out. When a device
                             * receives a push with an AAR in it, the application specified in the AAR
                             * is guaranteed to run. The AAR overrides the tag dispatch system.
                             * You can add it back in to guarantee that this
                             * activity starts when receiving a beamed message. For now, this code
                             * uses the tag dispatch system.
                            */
                            //,NdefRecord.createApplicationRecord("com.example.android.beam")
     /*               });
        }*/
        return msg;
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SENT:
                    Toast.makeText(getApplicationContext(), "Message sent!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        //mInfoText.setText(new String(msg.getRecords()[0].getPayload()));
       // Snackbar.make(main, new String(msg.getRecords()[0].getPayload()), Snackbar.LENGTH_LONG)
       //         .setAction("Action", null).show();

// read from byte array
        ByteArrayInputStream bais = new ByteArrayInputStream(msg.getRecords()[0].getPayload());
        DataInputStream in = new DataInputStream(bais);
        try {
            while (in.available() > 0) {
                String element = in.readUTF();
                log("byte array sent: "+element);

                sendFBItem(element);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     *
     * @param mimeType
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_return, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            mFirebaseAuth.signOut();
            loadLogInView();
        }

        return super.onOptionsItemSelected(item);
    }

}
