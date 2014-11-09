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

    private static final String TAG = "ColorListAdapter";
    private static final int blue = 0xFF0099CC;
    private static final int blue_pressed = 0xFF33B5E5;
    private static final int purple = 0xFF9933CC;
    private static final int purple_pressed = 0xFFAA66CC;
    private static final int green = 0xFF669900;
    private static final int green_pressed = 0xFF99CC00;
    private static final int orange = 0xFFFF8800;
    private static final int orange_pressed = 0xFFFFBB33;
    private static final int red = 0xFFCC0000;
    private static final int red_pressed = 0xFFFF4444;

    private static int[] COLORS = {blue, purple, green, orange, red};
    private static int[] COLORS_PRESSED = {blue_pressed, purple_pressed, green_pressed, orange_pressed, red_pressed};

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

    public void setLoading(int position, int visibility) {
        ViewHolder viewHolder = allViews.get(position);
        if (viewHolder.loading!=null)
            viewHolder.loading.setVisibility(visibility);
        allViews.get(position).loadingVisibility = visibility;
    }
}
