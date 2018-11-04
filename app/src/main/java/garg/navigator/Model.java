package garg.navigator;

public class Model {
    private String mHtmlDirection, mTime;
    private Directions mDirection;

    public Model(String html, String time, Directions direction){
        mHtmlDirection = html;
        mTime = time;
        mDirection = direction;
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
}
