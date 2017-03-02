package com.krkeco.dateit;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.zxing.WriterException;
import com.krkeco.dateit.FireBase.LogInActivity;
import com.krkeco.dateit.admob.AdMob;
import com.krkeco.dateit.widget.WidgetProvider;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;

import static com.krkeco.dateit.PrefHelper.checkKey;

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

    String EVENT_ID = "event";
    String DB_ID = "dateit";
    String TITLE_ID = "title";
    String LOCATION = "location";
    Context mContext;
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

    public static GoogleAccountCredential mCredential;
    private PrefHelper prefs;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    public long start_date,end_date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        prefs = new PrefHelper(mContext);

        compiledList = new ArrayList<>();
        if(attendeeList==null){
            attendeeList = new ArrayList<>();
        }

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
            start_date = getIntent().getLongExtra("start",0);
            end_date = getIntent().getLongExtra("end",0);
            // uploadIntentToFB();

        }
    }

    public void uploadIntentToFB(){

        Intent intent = getIntent();
        if(intent.hasExtra("data")) {
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

    public void initLayout(){

        checkKey(prefs.HOST_KEY);

        setContentView(R.layout.activity_return);
        mInfoText = (TextView) findViewById(R.id.intro_return);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        main = (LinearLayout) findViewById(R.id.return_llayout);

        CheckBox hostBox = (CheckBox) findViewById(R.id.host_check_box);
        TextView hostTV = (TextView) findViewById(R.id.host_textview);
        LinearLayout hostET = (LinearLayout) findViewById(R.id.host_layout);
        if(checkKey(prefs.HOST_KEY)){
            hostBox.setChecked(true);
            hostTV.setText(R.string.host_string_yes);
            hostET.setVisibility(View.VISIBLE);
        }

        hostBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                TextView hostTV = (TextView) findViewById(R.id.host_textview);
                LinearLayout hostET = (LinearLayout) findViewById(R.id.host_layout);
                if(isChecked){
                    prefs.setKey(prefs.HOST_KEY,true);
                    hostTV.setText(R.string.host_string_yes);
                    hostET.setVisibility(View.VISIBLE);
                }else{
                    prefs.setKey(prefs.HOST_KEY,false);
                    hostTV.setText(R.string.host_string);
                    hostET.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Set up ListView
        final ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listView.setAdapter(adapter1);
        CoordinatorLayout main_return = (CoordinatorLayout) findViewById(R.id.main_activity_return);

        if(prefs.checkKey(prefs.SENT_KEY) && prefs.checkKey(prefs.HOST_KEY)){
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_schedule_white_48dp));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, R.string.loading, Snackbar.LENGTH_LONG)
                           // .setAction("Action", null)
                    .show();

                    findFreeTime();

                    setListToFreeTime();

                    prefs.setKey(prefs.SENT_KEY, false);

                    emptyDataBase(EVENT_ID);

                    if (attendeeList != null) {
                    Snackbar.make(main, R.string.more_attendance_needed, Snackbar.LENGTH_LONG)
                           // .setAction("Action", null)
                            .show();

                     }
                }
            });

        }else {

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, R.string.loading, Snackbar.LENGTH_LONG)
                            //.setAction("Action", null)
                            .show();

                    adMob.showInterstitial();

                }
            });

        }

        if (mFirebaseUser == null) {
            // Not logged in, launch the Log In activity
            loadLogInView();
        } else {
            mUserId = mFirebaseUser.getUid();

             final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);

            mDatabase.child(DB_ID).child(mUserId).child(EVENT_ID).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String dataValue = dataSnapshot.child(TITLE_ID).getValue().toString();

                    adapter.add(dataValue);
                    addToList(dataValue);
                    prefs.setKey(prefs.SENT_KEY,true);
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

    public void emptyDataBase(String event){
        mDatabase.child(DB_ID).child(mUserId).child(event).setValue(null);
    }

    public void setListToFreeTime(){
        main = (LinearLayout) findViewById(R.id.return_llayout);
        String[] freetime = new String[freeList.size()];
        for(int x =0 ; x<freetime.length;x++){
            freetime[x] = freeList.get(x).getReadableDate();
        }

        // Set up ListView
        final ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1,freetime);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                setCalendarEvent(position);


            }
        });

    }

    public void createQRCode(String string){
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        String savePath = Environment.getExternalStorageDirectory().getPath() + "/QRCode/";
        // Initializing the QR Encoder with your value to be encoded, type you required and Dimension
        QRGEncoder qrgEncoder = new QRGEncoder(string, null, QRGContents.Type.TEXT, smallerDimension);
        try {
            // Getting QR-Code as Bitmap
            Bitmap  bitmap = qrgEncoder.encodeAsBitmap();
            // Setting Bitmap to ImageView
            ImageView qrImage = (ImageView) findViewById(R.id.qr_image_view);
            qrImage.setImageBitmap(bitmap);
            // Save with location, value, bitmap returned and type of Image(JPG/PNG).
            QRGSaver.save(savePath, string, bitmap, QRGContents.ImageType.IMAGE_JPEG);

        } catch (WriterException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    public void setCalendarEvent(int position){

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.setType("vnd.android.cursor.item/event");

        EditText titleText = (EditText) findViewById(R.id.return_host_title_text);
        String title =  titleText.getText().toString();
        intent.putExtra(CalendarContract.Events.TITLE,title);

        EditText locationText = (EditText) findViewById(R.id.return_host_location_text);
        String location =locationText.getText().toString();
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION,location);

        EditText durationText = (EditText) findViewById(R.id.event_duration);
        String duration =durationText.getText().toString();
        long duration_long = Long.parseLong(duration)*60000;

        Context  context = getApplicationContext();
        long event_id = getNewEventId(context.getContentResolver());
        intent.putExtra(CalendarContract.Events._ID, event_id);
        prefs.setKey(prefs.EVENT_KEY,event_id);

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,freeList.get(position).getStart());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,freeList.get(position).getStart()+duration_long);
        prefs.setKey(prefs.EVENT_NAME_KEY,EVENT_ID);
        prefs.setKey(prefs.EVENT_START_KEY,freeList.get(position).getStart());
        prefs.setKey(prefs.EVENT_END_KEY,freeList.get(position).getFinish());

        StringBuilder stringBuilder = new StringBuilder();
        if(attendeeList != null){
            for (int x = 0; x < attendeeList.size(); x++) {
                stringBuilder.append(attendeeList.get(x));
                if (x < attendeeList.size() - 1) {
                    stringBuilder.append(", ");
                }
            }
        }

        String attendance = stringBuilder.toString();
        intent.putExtra(Intent.EXTRA_EMAIL, attendance);
        Date date = new Date(freeList.get(position).getStart());
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        String dateFormatted = formatter.format(date);


        createQRCode(title+" at "+location+" on "+ date);

        intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        startActivity(intent);


    }

    public long getNewEventId(ContentResolver cr) {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_CALENDAR ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.WRITE_CALENDAR},0 );
        }
        Cursor cursor = cr.query(CalendarContract.Events.CONTENT_URI, new String [] {"MAX(_id) as max_id"}, null, null, "_id");
        cursor.moveToFirst();
        long max_val = cursor.getLong(cursor.getColumnIndex("max_id"));
        return max_val+1;
    }

    public long getLastEventId(ContentResolver cr) {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_CALENDAR ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this,new String[]{Manifest.permission.WRITE_CALENDAR},0 );
        }
        Cursor cursor = cr.query(CalendarContract.Events.CONTENT_URI, new String [] {"MAX(_id) as max_id"}, null, null, "_id");
        cursor.moveToFirst();
        long max_val = cursor.getLong(cursor.getColumnIndex("max_id"));
        return max_val;
    }

    public void initGoogleCred() {

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    public void cleanupList(){
        for(int x = compiledList.size()-1; x>0;x--) {
            log(compiledList.get(x).getStart()+"\n"
            +compiledList.get(x).getFinish()+"\n"
            +compiledList.get(x).getReadableDate());

            if(compiledList.get(x).getStart().compareTo(compiledList.get(x).getFinish())==0) {

                compiledList.remove(x);
                log("removed "+compiledList.get(x).getReadableDate());
            }
        }


        //remove single sec event
        for(int x = compiledList.size()-1; x>0;x--) {

            if(compiledList.get(x).getStart()==compiledList.get(x).getFinish()
                    || compiledList.get(x).getFinish() == null) {
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
                log("removed: "+compiledList.get(x).getReadableDate()+"\n"
                +"merged into: "+compiledList.get(x-1).getReadableDate());
                compiledList.remove(x);
            }
        }
    }

    public void log(String string){
        Log.v("akrkeco",string);
    }

    public void findFreeTime(){

        freeList = new ArrayList<BasicEvent>();

        Collections.sort(compiledList);
        cleanupList();
       for(int x = 0; x<compiledList.size()-1;x++) {

            BasicEvent newVent = new BasicEvent(compiledList.get(x).getFinish(),compiledList.get(x+1).getStart());
            freeList.add(newVent);

        }

    }

    public void addToList(String newString){
        try {
            long start = Long.parseLong(newString.substring(1, 14));
            long end = Long.parseLong(newString.substring(17, 30));
            BasicEvent newBasicEvent = new BasicEvent(start, end);
            compiledList.add(newBasicEvent);

        }catch (NumberFormatException e){
            e.printStackTrace();
        }

    }

    public void initNFC(){

        mInfoText = (TextView) textView;// findViewById(R.id.intro_return_textview);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mInfoText = (TextView) textView;

            Toast.makeText(this, R.string.nfc_novail, Toast.LENGTH_SHORT).show();
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
        NdefMessage msg;

        if(calendarList != null){
            String text = calendarList.toString();// "please click the calendar FAB and select your settings to send";
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
                    });
        }else {
            Snackbar.make(main, R.string.calendar_setup_error
                    , Snackbar.LENGTH_LONG)
                   // .setAction("Action", null)
                    .show();

            msg = null;
        }
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
                    Toast.makeText(getApplicationContext(), R.string.sent, Toast.LENGTH_LONG).show();
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
        //check if it returned from calendar app
        long prev_id = getLastEventId(getContentResolver());
        if (prev_id == prefs.getKey(prefs.EVENT_KEY)) {
            WidgetProvider.setText(this.getApplicationContext());
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

        mInfoText = (TextView) findViewById(R.id.intro_return);
        mInfoText.setText(new String(msg.getRecords()[0].getPayload()));

        String message = new String(msg.getRecords()[0].getPayload());

        int start=0;
        int finish;
        boolean first = true;

        for(int x = 0; x< message.length();x++){
            if(message.charAt(x) == ','){
                finish = x;
                if(first == false) {
                    String substring = message.substring(start, finish);
                    sendFBItem(substring);
                    start = x + 2;
                }else{//get email out of first line
                    first = false;
                    String substring = message.substring(start+1, finish);
                    start = x+2;
                    attendeeList.add(substring);

                }

            }
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
