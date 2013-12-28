package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class LogAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private SharedPreferences mPrefs;
    private List<LogLine> mData;

    private int mTextSize;
    private int mTimeViewWidth;
    private int mTagViewWidth;
    private float mViewAlpha;
    private int mBackgroundColor;

    public LogAdapter(Context context, List<LogLine> objects) {
        mContext = context.getApplicationContext();
        mData = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        readPrefs();
    }

    public void setData(List<LogLine> objects) {
        mData = objects;
        notifyDataSetChanged();
    }

    public void updateAppearance() {
        readPrefs();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public LogLine getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_log, parent, false);
            holder = new ViewHolder();
            holder.time = (TextView) convertView.findViewById(R.id.time);
            holder.tag = (TextView) convertView.findViewById(R.id.tag);
            holder.msg = (TextView) convertView.findViewById(R.id.msg);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final LogLine line = getItem(position);
        final int color = line.getColor();

        holder.time.setText(line.getTime());
        holder.tag.setText(line.getTag());
        holder.msg.setText(line.getMessage());

        holder.time.setTextColor(color);
        holder.tag.setTextColor(color);
        holder.msg.setTextColor(color);

        holder.time.setTextSize(mTextSize);
        holder.tag.setTextSize(mTextSize);
        holder.msg.setTextSize(mTextSize);

        holder.time.setWidth(mTimeViewWidth);
        holder.tag.setWidth(mTagViewWidth);

        convertView.setAlpha(mViewAlpha);

        return convertView;
    }

    private void readPrefs() {
        int dp = 6 + mPrefs.getInt(mContext.getString(R.string.pref_text_size), 0);
        mTextSize = dp;
        if (mTextSize <= 6) {
            mTimeViewWidth = dipToPixel(40);
            mTagViewWidth = dipToPixel(40);
        } else if (mTextSize <=8) {
            mTimeViewWidth = dipToPixel(60);
            mTagViewWidth = dipToPixel(60);
        } else {
            mTimeViewWidth = dipToPixel(80);
            mTagViewWidth = dipToPixel(80);
        }
        int v = mPrefs.getInt(mContext.getString(R.string.pref_text_opacity), 0);
        mViewAlpha =  0.3f + (float) v/100;

        Log.i("LOG", "pref=" + (mPrefs.getInt(mContext.getString(R.string.pref_text_opacity), 0)));
        Log.i("LOG", "alpha=" + mViewAlpha);
    }

    private int dipToPixel(int value) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (value * scale + 0.5f);
    }

    private class ViewHolder {
        public TextView time;
        public TextView tag;
        public TextView msg;
    }
}
