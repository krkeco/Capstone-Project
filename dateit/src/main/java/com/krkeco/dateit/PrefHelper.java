package com.krkeco.dateit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by KC on 3/1/2017.
 */

public class PrefHelper {

    public static Context context;

    public String HOST_KEY = "host";
    public String QR_KEY = "qrcode";
    public String SENT_KEY = "sent";
    public String EVENT_KEY = "eventid";
    public String EVENT_START_KEY = "eventstart";
    public String EVENT_NAME_KEY = "eventname";
    public String EVENT_END_KEY = "eventend";

    public PrefHelper(Context mcontext){
        context = mcontext;
    }

    public static boolean checkKey(String key){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit", Context.MODE_PRIVATE);
        boolean host = sharedPref.getBoolean(key,false);
        log("host is: "+host);

        return host;
    }
    public static long getKey(String key){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit",Context.MODE_PRIVATE);
        long host = sharedPref.getLong(key,-1);
       return host;
    }

    public static String getKeyString(String key){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit",Context.MODE_PRIVATE);
        String host = sharedPref.getString(key,null);
        return host;
    }


    public static void setKey(String key,Boolean host){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key,host);
        editor.commit();

    }
    public static void setKey(String key,String host){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key,host);
       editor.commit();

    }
    public static void setKey(String key,long host){

        SharedPreferences sharedPref = context.getSharedPreferences("dateit",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(key,host);
       editor.commit();

    }

    public static void log(String string){
        Log.v("akrkeco",string);
    }
}
