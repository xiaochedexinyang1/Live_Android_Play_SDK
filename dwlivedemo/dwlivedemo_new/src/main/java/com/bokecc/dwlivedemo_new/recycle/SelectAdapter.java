package com.bokecc.dwlivedemo_new.recycle;

import android.content.Context;

/**
 * 作者 ${郭鹏飞}.<br/>
 */

public abstract class SelectAdapter<VH extends BaseRecycleAdapter.BaseViewHolder, T> extends BaseRecycleAdapter<VH, T> {

    protected int mSelPosition = 0; // 选中的位置 默认第一个

    public SelectAdapter(Context context) {
        super(context);
    }

    public void setSelPosition(int selPosition) {
        mSelPosition = selPosition;
        notifyDataSetChanged();
    }
}
