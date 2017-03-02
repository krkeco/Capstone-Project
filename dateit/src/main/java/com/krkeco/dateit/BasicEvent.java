package com.krkeco.dateit;

import java.util.Calendar;

/**
 * Created by KC on 2/26/2017.
 */

public class BasicEvent implements Comparable<BasicEvent>{
    public Long start;
    public Long finish;

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getFinish() {
        return finish;
    }

    public void setFinish(Long finish) {
        this.finish = finish;
    }


    public BasicEvent(long mstart, long mfinish){
        start = mstart;
        finish = mfinish;
    }

    @Override
    public int compareTo(BasicEvent compareBasicEvent) {
        int compareQuantity = ((BasicEvent) compareBasicEvent).getStart().intValue();

        //ascending order
        return this.start.intValue() - compareQuantity;
    }


    public String getReadableDate(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(start);

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
        calendar2.setTimeInMillis(finish);

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
}
