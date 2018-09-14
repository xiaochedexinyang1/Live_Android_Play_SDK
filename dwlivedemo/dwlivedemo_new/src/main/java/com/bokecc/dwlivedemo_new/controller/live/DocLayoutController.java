package com.bokecc.dwlivedemo_new.controller.live;

import android.content.Context;
import android.view.View;

import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.sdk.mobile.live.widget.DocView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 直播文档布局区域控制类
 */
public class DocLayoutController extends BaseLayoutController {

    @BindView(R.id.live_doc)
    DocView mDocView;

    private Context mContext;

    public DocLayoutController(Context context, View view) {
        mContext = context;
        ButterKnife.bind(this, view);
    }

    public DocView getDocView() {
        return mDocView;
    }
}
