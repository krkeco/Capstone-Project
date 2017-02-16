package com.krkeco.dateit.FireBase;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FBIdService extends FirebaseInstanceIdService {
    public FBIdService() {
    }
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("krkeco", "Refreshed token: " + refreshedToken);

        // TODO: Implement this method to send any registration to your app's servers.
       // sendRegistrationToServer(refreshedToken);
    }
}
