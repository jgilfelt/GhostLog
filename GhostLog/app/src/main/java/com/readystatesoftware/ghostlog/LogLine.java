package com.readystatesoftware.ghostlog;

public class LogLine {

    private static final int DATE_INDEX = 0;
    private static final int TIME_INDEX = 1;
    private static final int PID_INDEX = 2;
    private static final int LEVEL_INDEX = 4;
    private static final int TAG_INDEX = 5;
    private static final int MSG_INDEX = 6;

    private String mDate;
    private String mTime;
    private String mLevel;
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
                case PID_INDEX:
                    mPid = 0;
                    break;
                case TAG_INDEX:
                    mTag = part;
                    break;
                default:
                    if (i >= MSG_INDEX) {
                        mMessage += (part + ((i == parts.length-1) ? "" :" "));
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

    public String getMessage() {
        return mMessage;
    }
}
