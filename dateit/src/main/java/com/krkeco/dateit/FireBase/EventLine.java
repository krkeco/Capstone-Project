package com.krkeco.dateit.FireBase;

import java.util.ArrayList;

/**
 * Created by KC on 2/24/2017.
 */

public class EventLine {


    public ArrayList<String> blackout_dates;


    public EventLine() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public EventLine(ArrayList<String> blackout_dates) {
        this.blackout_dates = blackout_dates;
    }
}
