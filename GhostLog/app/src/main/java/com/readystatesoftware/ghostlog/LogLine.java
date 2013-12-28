package com.readystatesoftware.ghostlog;

import android.graphics.Color;

public class LogLine {

    private static final int DATE_INDEX = 0;
    private static final int TIME_INDEX = 1;
    private static final int PID_INDEX = 2;
    private static final int PKG_INDEX = 3;
    private static final int LEVEL_INDEX = 4;
    private static final int TAG_INDEX = 5;
    private static final int MSG_INDEX = 6;

    public static final String LEVEL_VERBOSE = "V";
    private static final String LEVEL_DEBUG = "D";
    private static final String LEVEL_INFO = "I";
    private static final String LEVEL_WARN = "W";
    private static final String LEVEL_ERROR = "E";
    private static final String LEVEL_ASSERT = "A";

    private String mDate;
    private String mTime;
    private String mLevel;
    private String mPackage;
    private int mPid;
    private String mTag;
    private String mMessage;

    public LogLine(String raw) {

        String[] parts = raw.split(":? +");
        mMessage = "";
        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            switch (i) {
                case DATE_INDEX:
                    mDate = part;
                    break;
                case TIME_INDEX:
                    mTime = part;
                    break;
                case LEVEL_INDEX:
                    mLevel = part;
                    break;
                case PKG_INDEX:
                    mPackage = part;
                    break;
                case PID_INDEX:
                    mPid = 0;
                    break;
                case TAG_INDEX:
                    mTag = part;
                    break;
                default:
                    if (i >= MSG_INDEX) {
                        mMessage += (part + ((i == parts.length-1 || part.length() == 0) ? "" :" "));
                    }
            }

        }

    }

    public String getDate() {
        return mDate;
    }

    public String getTime() {
        return mTime;
    }

    public String getLevel() {
        return mLevel;
    }

    public int getPid() {
        return mPid;
    }

    public String getTag() {
        return mTag;
    }

    public String getPackage() {
        return mPackage;
    }

    public String getMessage() {
        return mMessage;
    }

    public int getColor() {
        if(LEVEL_VERBOSE.equals(mLevel)) {
            return Color.parseColor("#EEEEEE");
        } else if(LEVEL_DEBUG.equals(mLevel)) {
            return Color.parseColor("#4D4DFF");
        } else if(LEVEL_INFO.equals(mLevel)) {
            return Color.parseColor("#5BBD00");
        } else if(LEVEL_WARN.equals(mLevel)) {
            return Color.parseColor("#FFD042");
        } else if(LEVEL_ERROR.equals(mLevel)) {
            return Color.parseColor("#FF4D4D");
        } else if(LEVEL_ASSERT.equals(mLevel)) {
            return Color.parseColor("#FF42D0");
        } else {
            return Color.parseColor("#EEEEEE");
        }
    }

}
