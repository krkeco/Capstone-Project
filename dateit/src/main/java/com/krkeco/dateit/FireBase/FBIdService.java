package com.krkeco.dateit.FireBase;

import android.app.Activity;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FBIdService extends FirebaseInstanceIdService {
    static Activity mActivity;
    static String refreshedToken;
    public FBIdService() {
    }
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.v("krkeco", "Refreshed token: " + refreshedToken);

        // TODO: Implement this method to send any registration to your app's servers.
       // sendRegistrationToServer(refreshedToken);


    }
/*
    public static String getToken(Activity activity){

        String accountName = getAccount(activity);

        // Initialize the scope using the client ID you got from the Console.
        final String scope = "audience:server:client_id:"
                + "323308485893-9ca1gml55dnnins5188mikuou3i4q4do.apps.googleusercontent.com";
        String idToken = null;
        try {
            idToken = GoogleAuthUtil.getToken(activity, accountName, scope);
            Log.v("krkeco","token is: "+idToken);
        } catch (Exception e) {
            Log.v("krkeco","exception while getting idToken: " + e);
        }
        return idToken;
    }

    // This snippet takes the simple approach of using the first returned Google account,
// but you can pick any Google account on the device.
    public static String getAccount(Activity activity) {

        mActivity = activity;
        Account[] accounts = AccountManager.get(mActivity.getApplicationContext()).
                getAccountsByType("com.google");
        if (accounts.length == 0) {
            Log.v("krkeco","returned null");
            return null;
        }
        Log.v("krkeco","account name: "+accounts[0].name);
        return accounts[0].name;

    }*/
}
