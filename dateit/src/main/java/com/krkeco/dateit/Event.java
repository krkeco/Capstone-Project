package com.krkeco.dateit;

import java.util.Date;

/**
 * Created by KC on 2/26/2017.
 */

public class Event implements Comparable<Event>{
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


    public Event(long mstart, long mfinish){
        start = mstart;
        finish = mfinish;
    }

    @Override
    public int compareTo(Event compareEvent) {
        int compareQuantity = ((Event) compareEvent).getStart().intValue();

        //ascending order
        return this.start.intValue() - compareQuantity;
    }
}
