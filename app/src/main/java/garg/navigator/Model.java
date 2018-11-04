package garg.navigator;

import android.location.Location;

public class Model {
    private String mHtmlDirection, mTime;
    private Directions mDirection;
    private Location mLocation;

    public Model(String html, String time, Directions direction, Location location){
        mHtmlDirection = html;
        mTime = time;
        mDirection = direction;
        mLocation = location;
    }

//    private void setDirection(String direction){
//        mHtmlDirection = direction;
//    }

//    private void setTime(String time){
//        mTime = time;
//    }

    public String getHtmlDirection(){
        return mHtmlDirection;
    }

    public String getTime(){
        return mTime;
    }

    public Directions getDirection() {
        return mDirection;
    }

    public Location getLocation(){ return mLocation; }
}
