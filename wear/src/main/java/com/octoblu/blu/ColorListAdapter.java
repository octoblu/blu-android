package com.octoblu.blu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ColorListAdapter extends ArrayAdapter<String>{
    private static int[] COLORS = {R.color.blue, R.color.purple, R.color.green, R.color.orange, R.color.red};
    private static int[] COLORS_PRESSED = {R.color.blue_pressed, R.color.purple_pressed, R.color.green_pressed, R.color.orange_pressed, R.color.red_pressed};

    private Context context;
    private int resource;

    public ColorListAdapter(Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.resource = resource;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        String triggerName = getItem(position);

        View view = convertView;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resource, parent, false);
        }

        final TextView textView = (TextView) view.findViewById(R.id.triggerName);
        textView.setBackgroundColor(getColorForPosition(position));
        textView.setText(triggerName);

        return view;
    }
    private int getColorForPosition(int position) {
        int index = position % COLORS.length;

        return context.getResources().getColor(COLORS[index]);
    }
}
