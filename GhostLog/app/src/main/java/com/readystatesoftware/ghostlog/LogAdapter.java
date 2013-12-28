package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class LogAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<LogLine> mData;

    public LogAdapter(Context context, List<LogLine> objects) {
        mData = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<LogLine> objects) {
        mData = objects;
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

        return convertView;
    }

    private class ViewHolder {
        public TextView time;
        public TextView tag;
        public TextView msg;
    }
}
