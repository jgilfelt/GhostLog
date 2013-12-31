package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
    private float mDensity;

    private int mTextSize;
    private int mTimeViewWidth;
    private int mTagViewWidth;
    private float mViewAlpha;

    public LogAdapter(Context context, List<LogLine> objects) {
        mContext = context.getApplicationContext();
        mData = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mDensity = mContext.getResources().getDisplayMetrics().density;
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

        ViewGroup.LayoutParams lp = holder.time.getLayoutParams();
        lp.width = mTimeViewWidth;
        holder.time.setLayoutParams(lp);

        lp = holder.tag.getLayoutParams();
        lp.width = mTagViewWidth;
        holder.tag.setLayoutParams(lp);

        convertView.setAlpha(mViewAlpha);

        return convertView;
    }

    private void readPrefs() {
        int dp = 6 + mPrefs.getInt(mContext.getString(R.string.pref_text_size), 0);
        mTextSize = dp;
        if (mTextSize <= 6) {
            mTimeViewWidth = dipToPixel(44);
            mTagViewWidth = dipToPixel(52);
        } else if (mTextSize <= 7) {
            mTimeViewWidth = dipToPixel(52);
            mTagViewWidth = dipToPixel(60);
        } else if (mTextSize <= 8) {
            mTimeViewWidth = dipToPixel(64);
            mTagViewWidth = dipToPixel(64);
        } else if (mTextSize <= 9) {
            mTimeViewWidth = dipToPixel(72);
            mTagViewWidth = dipToPixel(72);
        } else if (mTextSize <= 10) {
            mTimeViewWidth = dipToPixel(80);
            mTagViewWidth = dipToPixel(76);
        } else {
            mTimeViewWidth = dipToPixel(88);
            mTagViewWidth = dipToPixel(76);
        }
        int v = mPrefs.getInt(mContext.getString(R.string.pref_text_opacity), 0);
        mViewAlpha =  0.3f + (float) v/100;
    }

    private int dipToPixel(int value) {
        return (int) (value * mDensity + 0.5f);
    }

    private class ViewHolder {
        public TextView time;
        public TextView tag;
        public TextView msg;
    }
}
