package garg.navigator;

/**
 * Created by Shivam Garg on 08-10-2018.
 */

public class Model {
    private String mDirection, mTime;

    public Model(String direction, String time){
        mDirection = direction;
        mTime = time;
    }

    private void setDirection(String direction){
        mDirection = direction;
    }

    private void setTime(String time){
        mTime = time;
    }

    public String getDirection(){
        return mDirection;
    }

    public String getTime(){
        return mTime;
    }

}
