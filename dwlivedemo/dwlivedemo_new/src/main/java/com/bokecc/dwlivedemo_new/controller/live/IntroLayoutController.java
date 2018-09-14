package com.bokecc.dwlivedemo_new.controller.live;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.dwlivedemo_new.view.MixedTextView;
import com.bokecc.sdk.mobile.live.DWLive;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 直播简介布局区域控制类
 */
public class IntroLayoutController extends BaseLayoutController {

    @BindView(R.id.tv_intro_title)
    TextView title;

    @BindView(R.id.content_layer)
    LinearLayout content_layer;

    private Context mContext;

    public IntroLayoutController(Context context, View view) {
        mContext = context;
        ButterKnife.bind(this, view);
    }

    public void initIntro() {
        if (DWLive.getInstance().getRoomInfo() != null) {
            title.setText(DWLive.getInstance().getRoomInfo().getName());
            content_layer.removeAllViews();
            content_layer.addView(new MixedTextView(mContext, DWLive.getInstance().getRoomInfo().getDesc()));
        }
    }
}