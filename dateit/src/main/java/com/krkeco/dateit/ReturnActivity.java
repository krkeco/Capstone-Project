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
import java.util.Calendar;
import java.util.Collections;

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

    String EVENT_ID = "event";//Long.toString(System.currentTimeMillis());
    String DB_ID = "dateit";
    String TITLE_ID = "title";
    /**
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

     **/
    ArrayList<String> calendarList;
    ArrayList<Event> compiledList, freeList;
    public boolean settingsUp = false;
    public long start_date,end_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        compiledList = new ArrayList<>();
        initAdmob();

        initFireBaseAuth();

        checkIntentForDB();

        initLayout();

        initNFC();

        uploadIntentToFB();

    }

    public void checkIntentForDB(){

        Intent intent = getIntent();
        if(intent.hasExtra("data")) {

            settingsUp = true;
            log("settings are up");
            start_date = getIntent().getLongExtra("start",0);
            end_date = getIntent().getLongExtra("end",0);

        }
    }

    public void uploadIntentToFB(){

        if(settingsUp == true) {
            calendarList = getIntent().getStringArrayListExtra("data");

            for(int x = 0; x < calendarList.size(); x++) {

                sendFBItem(calendarList.get(x));

             }

        }
    }

    public void sendFBItem(String string){//this is so all sends follow same rules in db
        com.krkeco.dateit.FireBase.Item item = new com.krkeco.dateit.FireBase.Item(string);
        mDatabase.child(DB_ID).child(mUserId).child(EVENT_ID).push().setValue(item);

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

        if(settingsUp==true){

            log("we went through to new fab");
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_schedule_white_48dp));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Loading, please wait", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    Collections.sort(compiledList);

                    long fixend = end_date;
                    long fixstart = start_date;

                    freeList = new ArrayList<Event>();
                    //only slots for time inside of selection
                  //  int val =(int) (fixend-fixstart);

                    //assume busy until proven free
                /*    Boolean[] freetime = new Boolean[val];
                    for(int f = 0; f<val;f++){
                        freetime[f]=false;
                    }*/

                    //merge adjacent events
                    for(int x = compiledList.size()-1; x>1;x--) {
                        if(compiledList.get(x).getStart()<=compiledList.get(x-1).getFinish()){
                            compiledList.get(x).setStart(compiledList.get(x-1).getStart());
                            compiledList.remove(x-1);
                        }
                        log("merged: "+compiledList.get(x-1).getStart()+" to "+compiledList.get(x-1).getFinish());


                    }
                    //convert millis to minutes for events
                    for(int x = 0; x<compiledList.size()-1;x++) {
                        // divide 60000 and subtract fixstart for constant
                        compiledList.get(x).setStart(compiledList.get(x).getStart());
                        compiledList.get(x).setFinish(compiledList.get(x).getFinish());
                        log("event"+x+": "+compiledList.get(x).getStart()+" to "+compiledList.get(x).getFinish());

                    }

                    if(compiledList.get(0).getStart()>=fixstart){
                        for(int x = 0; x<compiledList.get(0).getStart();x++){

                            Event newVent = new Event(fixstart,compiledList.get(0).getStart());
                            freeList.add(newVent);
                            log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
                        }
                    }else{log("no freetime before breakfast");}

                    for(int x = 0; x<compiledList.size()-1;x++) {

                        Event newVent = new Event(compiledList.get(x).getFinish(),compiledList.get(x+1).getStart());
                        freeList.add(newVent);
                      //  log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
                        getStartDate(newVent.getStart(),newVent.getFinish());
                     /*   for(int f = compiledList.get(x).getFinish().intValue(); f < compiledList.get(x+1).getStart().intValue();f++){
                            freetime[f] = true;
                            log("free time at:"+f);
                        }*/
                    }

                    if(compiledList.get(compiledList.size()-1).getFinish() <fixend){
                       /* for(int l =compiledList.get(compiledList.size()-1).getFinish().intValue(); l<freetime.length;l++){
                            freetime[l]=true;
                            log("free time at:"+l);
                        }*/

                        Event newVent = new Event(compiledList.get(compiledList.size()-1).getFinish(),fixend);
                        freeList.add(newVent);

                        //log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
                    }else{log("no freetime after dinner");}
/*
                    boolean bounce =false;
                    long start = 0;
                    long end;
                    freeList = new ArrayList<Event>();
                    for(int x = 0; x < freetime.length; x++){

                        if(freetime[x]==true
                                && bounce == false){
                           start =  (x+start_date);
                            bounce = true;
                        }
                        if(freetime[x]==false
                                && bounce == true){
                            bounce = false;
                            end = (x+start_date);
                            Event newVent = new Event(start,end);
                            freeList.add(newVent);
                            log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
                        }

                    }
*/

                }
            });
        }else {

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Loading, please wait", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    adMob.showInterstitial();

                }
            });

        }
        //get bundle/details and create separate textbox for each date with onclick to add to calendar
        main = (LinearLayout) findViewById(R.id.return_llayout);


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
            mDatabase.child(DB_ID).child(mUserId).child(EVENT_ID).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String dataValue = dataSnapshot.child(TITLE_ID).getValue().toString();

                    adapter.add(dataValue);
                    addToList(dataValue);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    adapter.remove((String) dataSnapshot.child(TITLE_ID).getValue());
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
    public void getStartDate(long millisstart, long millisfinish){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisstart);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);


        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(millisfinish);

        int mYear2 = calendar2.get(Calendar.YEAR);
        int mMonth2 = calendar2.get(Calendar.MONTH);
        int mDay2 = calendar2.get(Calendar.DAY_OF_MONTH);
        int hour2 = calendar2.get(Calendar.HOUR);
        int minute2 = calendar2.get(Calendar.MINUTE);

        log("start:"+mYear+"/"+mMonth+"/"+mDay+" "+hour+":"+minute+"\n"+
                "finish:"+mYear2+"/"+mMonth2+"/"+mDay2+" "+hour2+":"+minute2);
    }

    public void addToList(String newString){
        long start = Long.parseLong(newString.substring(1,14));
        long end = Long.parseLong(newString.substring(17,30));
        Event newEvent = new Event(start,end);
        compiledList.add(newEvent);

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
