package com.octoblue.blu.shared;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ColorListAdapter extends ArrayAdapter<String>{

    private static int[] COLORS = {
            0xff9c27b0,
            0xff3f51b5,
            0xff03a9f4,
            0xff009688,
            0xff8bc34a,
            0xffffc107,
            0xfff44336,
    };

    private Context context;
    private int resource;
    private int triggerName;
    private int triggerLoading;

    private SparseArray<ViewHolder> allViews;

    private static class ViewHolder {
        TextView name;
        RelativeLayout loading;
        int loadingVisibility;
    }

    public ColorListAdapter(Context context, int resource, int triggerName, int triggerLoading) {
        super(context, resource);
        this.context = context;
        this.resource = resource;
        this.triggerName = triggerName;
        this.triggerLoading = triggerLoading;
        allViews = new SparseArray<ViewHolder>();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        String triggerName = getItem(position);

        ViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resource, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(this.triggerName);
            if (triggerLoading != -1)
                viewHolder.loading = (RelativeLayout) convertView.findViewById(this.triggerLoading);
            viewHolder.loadingVisibility = View.GONE;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        int bgColor = getColorForPosition(position);
        if (triggerLoading != -1) {
            viewHolder.loading.setBackgroundColor(bgColor);
            viewHolder.loading.setVisibility(viewHolder.loadingVisibility);
        }
        viewHolder.name.setBackgroundColor(bgColor);
        viewHolder.name.setText(triggerName);
        allViews.put(position,viewHolder);

        return convertView;
    }

    private int getColorForPosition(int position) {
        return COLORS[position % COLORS.length];
    }

    public void setLoading(Integer position, int visibility) {
        if(position == null){
            return;
        }
        ViewHolder viewHolder = allViews.get(position);
        if (viewHolder == null) {
            return;
        }
        if (viewHolder.loading!=null) {
            viewHolder.loading.setVisibility(visibility);
        }
        allViews.get(position).loadingVisibility = visibility;
    }
}
