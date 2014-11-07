package com.octoblu.blu;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created by roy on 11/6/14.
 */
public class FlowRefreshLayout extends SwipeRefreshLayout {
    public FlowRefreshLayout(Context context) {
        super(context);
    }

    public FlowRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        ListView flowList = (ListView) findViewById(R.id.flowList);
        return (flowList.getScrollY() == 0.0);
    }
}
