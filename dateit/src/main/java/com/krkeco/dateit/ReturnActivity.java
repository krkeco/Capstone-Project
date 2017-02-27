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
import android.provider.CalendarContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.InterstitialAd;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
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
import java.util.Arrays;
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
    String LOCATION = "location";
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
    public static ArrayList<String> attendeeList;
    ArrayList<BasicEvent> compiledList, freeList;

    public boolean settingsUp = false;
    public long start_date,end_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        compiledList = new ArrayList<>();
        attendeeList = new ArrayList<>();

        initAdmob();

        initGoogleCred();

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


            for(int x = 1; x < calendarList.size(); x++) {//calendarlist[1] is email

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

        // Set up ListView
        final ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listView.setAdapter(adapter1);

        if(settingsUp==true){

            log("we went through to new fab");
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_schedule_white_48dp));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Loading, please wait", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    findFreeTime();

                    setListToFreeTime();
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
            //final ListView listView = (ListView) findViewById(R.id.listView);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
            // listView.setAdapter(adapter);

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

    public void setListToFreeTime(){
        main = (LinearLayout) findViewById(R.id.return_llayout);
        String[] freetime = new String[freeList.size()];
        for(int x =0 ; x<freetime.length;x++){
            freetime[x] = "you are free "+getStartDate(freeList.get(x).getStart(),freeList.get(x).getFinish());
            log(freetime[x]);
        }

        // Set up ListView
        final ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1,freetime);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setData(CalendarContract.Events.CONTENT_URI);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra(CalendarContract.Events.TITLE, EVENT_ID);
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, LOCATION);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,freeList.get(position).getStart());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,freeList.get(position).getFinish());
                intent.putExtra(Intent.EXTRA_EMAIL, "oslckc@gmail.com");
                intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
                startActivity(intent);
             /*   Event event = new Event()
                        .setSummary(EVENT_ID)
                        .setLocation(LOCATION);

                DateTime startDateTime = new DateTime(freeList.get(position).getStart());
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime);
                      //  .setTimeZone("America/Los_Angeles");
                event.setStart(start);

                DateTime endDateTime = new DateTime(freeList.get(position).getFinish());
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime);
                      //  .setTimeZone("America/Los_Angeles");
                event.setEnd(end);

                //String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
                //event.setRecurrence(Arrays.asList(recurrence));

                EventAttendee[] attendees = new EventAttendee[] {
                        new EventAttendee().setEmail("lpage@example.com"),
                        new EventAttendee().setEmail("sbrin@example.com"),
                };
                event.setAttendees(Arrays.asList(attendees));

                EventReminder[] reminderOverrides = new EventReminder[] {
                        new EventReminder().setMethod("email").setMinutes(24 * 60),
                        new EventReminder().setMethod("popup").setMinutes(10),
                };
                Event.Reminders reminders = new Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(Arrays.asList(reminderOverrides));
                event.setReminders(reminders);

                com.google.api.services.calendar.Calendar mService = null;
                HttpTransport transport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                mService = new com.google.api.services.calendar.Calendar.Builder(
                        transport, jsonFactory,mCredential)
                        .setApplicationName("Google Calendar API Android Quickstart")
                        .build();

                String calendarId = "primary";
                try {
                    event = mService.events().insert(calendarId, event).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log("Event created: %s\n"+event.getHtmlLink());*/
            }
        });

    }

     public static GoogleAccountCredential mCredential;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    public void initGoogleCred() {

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }
    public void cleanupList(){

        //merge adjacent events
        for(int x = compiledList.size()-1; x>0;x--) {
            log("merging: "+compiledList.get(x).getStart()+" "+compiledList.get(x).getFinish()+
                    "\n with:   "+compiledList.get(x-1).getStart()+" "+compiledList.get(x-1).getFinish());

            if(compiledList.get(x).getStart()==compiledList.get(x).getFinish()) {

                compiledList.remove(x);
            }

            //if events overlap, we combine int event(x-1) and remove event x
            if(compiledList.get(x).getStart()<=compiledList.get(x-1).getFinish()){

                if(compiledList.get(x).getStart()<compiledList.get(x-1).getStart()){

                    compiledList.get(x-1).setStart(compiledList.get(x).getStart());
                }

                if(compiledList.get(x).getFinish()>compiledList.get(x-1).getFinish()){

                    compiledList.get(x-1).setFinish(compiledList.get(x).getFinish());
                }

                compiledList.remove(x);
            }
            log("merged events; now: "+
                    "\n with:   "+compiledList.get(x-1).getStart()+" "+compiledList.get(x-1).getFinish());

        }
    }

    public void findFreeTime(){


        long fixend = end_date;
        long fixstart = start_date;

        freeList = new ArrayList<BasicEvent>();

        Collections.sort(compiledList);
        cleanupList();
        cleanupList();//can't figure out why I need this twice, but sometimes get messedup dates otherwise


        if(compiledList.get(0).getStart()>=fixstart){

            BasicEvent newVent = new BasicEvent(fixstart,compiledList.get(0).getStart());
            freeList.add(newVent);
            log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());

        }else{log("no freetime before breakfast");}

        for(int x = 0; x<compiledList.size()-1;x++) {

            BasicEvent newVent = new BasicEvent(compiledList.get(x).getFinish(),compiledList.get(x+1).getStart());
            freeList.add(newVent);
            log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
            getStartDate(newVent.getStart(),newVent.getFinish());
        }

        if(compiledList.get(compiledList.size()-1).getFinish() <fixend){
            BasicEvent newVent = new BasicEvent(compiledList.get(compiledList.size()-1).getFinish(),fixend);
            freeList.add(newVent);

            //log("event start: "+newVent.getStart()+" event finish: "+newVent.getFinish());
        }else{log("no freetime after dinner");}

    }

    public String getStartDate(long millisstart, long millisfinish){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisstart);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        if(hour == 0){
            hour =12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        String min;
        if(minute<10){
            min = "0"+minute;
        }else{
            min = Integer.toString(minute);
        }
        String ampm = "AM";
        int apmm = calendar.get(Calendar.AM_PM);
        if(apmm == 1){
            ampm = "PM";
        }

        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(millisfinish);

        int mYear2 = calendar2.get(Calendar.YEAR);
        int mMonth2 = calendar2.get(Calendar.MONTH);
        int mDay2 = calendar2.get(Calendar.DAY_OF_MONTH);
        int hour2 = calendar2.get(Calendar.HOUR);
        if(hour2 == 0){
            hour2 =12;
        }
        int minute2 = calendar2.get(Calendar.MINUTE);
        String min2;
        if(minute2<10){
            min2 = "0"+minute2;
        }else{min2 = Integer.toString(minute2);}
        String ampm2 = "AM";
        int apmm2 = calendar2.get(Calendar.AM_PM);
        if(apmm2 == 1){
            ampm2 = "PM";
        }
        String output;
        if(mYear == mYear2 && mMonth == mMonth2 && mDay == mDay2){
            output = +mYear+"/"+(mMonth+1)+"/"+mDay+"\n"+
                    hour+":"+min+" "+ampm+" to "+hour2+":"+min2+" "+ampm2;

        }else {
            output = +mYear + "/" + (mMonth + 1) + "/" + mDay + " " + hour + ":" + min + " " + ampm + " to "
                    + mYear2 + "/" + (mMonth2 + 1) + "/" + mDay2 + " " + hour2 + ":" + min2 + " " + ampm2;
        }
        return output;
    }

    public void addToList(String newString){
        long start = Long.parseLong(newString.substring(1,14));
        long end = Long.parseLong(newString.substring(17,30));
        BasicEvent newBasicEvent = new BasicEvent(start,end);
        compiledList.add(newBasicEvent);

    }

    public void initNFC(){

        mInfoText = (TextView) textView;// findViewById(R.id.intro_return_textview);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mInfoText = (TextView) textView;
            mInfoText.setText("NFC is not available on this device.");
            log("NFC not allowed on device");
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
            log("msg ="+calendarByteArray.toString());
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
        log("on resume");
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            log("being process intent");
            processIntent(getIntent());

        }else{
            log("resume was not due to nfc???");
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        log("got a new intent");
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    void processIntent(Intent intent) {
        log("inside process intent");
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
            boolean first = true;
            while (in.available() > 0) {
                log("default first is true");
                if(first == false){
                    log("first false");
                    String element = in.readUTF();
                    sendFBItem(element);

                }else{
                    String element = in.readUTF();
                    attendeeList.add(element);
                    log("we have an attendee!");
                    log(attendeeList.toString());
                    log(element);
                    first = false;
                }

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
