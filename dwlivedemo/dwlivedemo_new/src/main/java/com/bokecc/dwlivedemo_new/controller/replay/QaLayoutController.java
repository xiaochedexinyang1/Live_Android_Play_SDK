package com.bokecc.dwlivedemo_new.controller.replay;

import android.content.Context;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.adapter.LiveQaAdapter;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.dwlivedemo_new.global.QaInfo;
import com.bokecc.sdk.mobile.live.pojo.Answer;
import com.bokecc.sdk.mobile.live.pojo.Question;

import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * 回放问答布局区域控制类
 */
public class QaLayoutController extends BaseLayoutController{

    @BindView(R.id.rv_qa_container)
    RecyclerView mQaList;

    @BindView(R.id.rl_qa_input_layout)
    RelativeLayout mInputLayout;

    LiveQaAdapter mQaAdapter;

    int mQaInfoLength;

    Context mContext;

    public QaLayoutController(Context context, View view) {
        mContext = context;
        ButterKnife.bind(this, view);
        mQaInfoLength = 0;
        mInputLayout.setVisibility(View.GONE);
    }

    public void initQaLayout() {
        mQaList.setLayoutManager(new LinearLayoutManager(mContext));
        mQaAdapter = new LiveQaAdapter(mContext);
        mQaList.setAdapter(mQaAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
    }

    public void clearQaInfo() {
        mQaAdapter.resetQaInfos();
    }

    public void addReplayQAInfos(LinkedHashMap<String, QaInfo> replayQaInfos) {
        // 回放的问答内容随时间轴推进展示
        if (DWApplication.REPLAY_QA_FOLLOW_TIME) {
            // 如果数据长度没发生变化就不刷新
            if (mQaInfoLength != replayQaInfos.size()) {
                mQaAdapter.addReplayQuestoinAnswer(replayQaInfos);
                mQaList.scrollToPosition(replayQaInfos.size() - 1);
                mQaInfoLength = replayQaInfos.size();
            }
        } else {
            mQaAdapter.addReplayQuestoinAnswer(replayQaInfos);
        }
    }

    public void addQuestion(Question question) {
        mQaAdapter.addQuestion(question);
    }

    public void addAnswer(Answer answer) {
        mQaAdapter.addAnswer(answer);
    }
}
