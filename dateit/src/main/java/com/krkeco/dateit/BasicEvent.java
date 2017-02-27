package com.krkeco.dateit;

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
}
