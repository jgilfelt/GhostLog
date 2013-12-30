package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.graphics.Color;

public class LogLine {

    private static final int DATE_INDEX = 0;
    private static final int TIME_INDEX = 1;
    private static final int PID_INDEX = 2;
    private static final int TID_INDEX = 3;
    private static final int LEVEL_INDEX = 4;
    private static final int TAG_INDEX = 5;
    private static final int MSG_INDEX = 6;

    public static final String LEVEL_VERBOSE = "V";
    private static final String LEVEL_DEBUG = "D";
    private static final String LEVEL_INFO = "I";
    private static final String LEVEL_WARN = "W";
    private static final String LEVEL_ERROR = "E";
    private static final String LEVEL_ASSERT = "A";

    private String mRaw;

    private String mDate;
    private String mTime;
    private String mLevel;
    private int mPid;
    private int mTid;
    private String mTag;
    private String mMessage;

    public LogLine(String raw) {

        mRaw = raw;

        String[] parts = raw.split(":? +");
        StringBuffer sb = new StringBuffer();
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
                case TID_INDEX:
                    try {
                        mTid = Integer.parseInt(part.trim());
                    } catch (NumberFormatException e) {
                        mTid = 0;
                    }
                    break;
                case PID_INDEX:
                    try {
                        mPid = Integer.parseInt(part.trim());
                    } catch (NumberFormatException e) {
                        mPid = 0;
                    }
                    break;
                case TAG_INDEX:
                    mTag = part;
                    break;
                default:
                    if (i >= MSG_INDEX) {
                        sb.append(part + ((i == parts.length-1 || part.length() == 0) ? "" :" "));
                    }
            }

        }
        mMessage = sb.toString();

    }

    public String getRaw() {
        return mRaw;
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

    public int getTid() {
        return mTid;
    }

    public String getTag() {
        return mTag;
    }

    public String getMessage() {
        return mMessage;
    }

    public int getColor() {
        if(LEVEL_VERBOSE.equals(mLevel)) {
            return Color.parseColor("#EEEEEE");
        } else if(LEVEL_DEBUG.equals(mLevel)) {
            return Color.parseColor("#0099CC");
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

    public static String getLevelName(Context context, String code) {
        String[] codes = context.getResources().getStringArray(R.array.levels_values);
        String[] names = context.getResources().getStringArray(R.array.levels_entries);
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) {
                return names[i];
            }
        }
        return null;
    }

}
