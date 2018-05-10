package com.bokecc.dwlivedemo_new.recycle;

import android.support.v7.widget.RecyclerView;

/**
 * 作者 ${郭鹏飞}.<br/>
 */

public interface ITouchListener {
    void onClick(RecyclerView.ViewHolder viewHolder);
    void onLongPress(RecyclerView.ViewHolder viewHolder);
    void onTouchDown(RecyclerView.ViewHolder viewHolder);
    void onTouchUp(RecyclerView.ViewHolder viewHolder);
}
