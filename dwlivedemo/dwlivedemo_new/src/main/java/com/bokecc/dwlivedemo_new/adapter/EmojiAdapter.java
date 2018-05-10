package com.bokecc.dwlivedemo_new.adapter;

import android.content.Context;
import android.widget.ImageView;

import com.bokecc.dwlivedemo_new.R;

/**
 * 作者 ${郭鹏飞}.<br/>
 */

public class EmojiAdapter extends CommonArrayAdapter<Integer> {

    public EmojiAdapter(Context context) {
        super(context);
    }

    @Override
    protected void onBindViewHolder(ViewHolder viewHolder, int position) {
        ImageView emoji = viewHolder.getView(R.id.id_item_emoji);
        emoji.setImageResource(datas[position]);
    }

    @Override
    protected int getItemViewId() {
        return R.layout.item_emoji;
    }
}
