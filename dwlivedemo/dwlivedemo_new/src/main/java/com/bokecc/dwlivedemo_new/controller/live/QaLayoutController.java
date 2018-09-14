package com.bokecc.dwlivedemo_new.controller.live;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.adapter.LiveQaAdapter;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.sdk.mobile.live.DWLive;
import com.bokecc.sdk.mobile.live.pojo.Answer;
import com.bokecc.sdk.mobile.live.pojo.Question;

import org.json.JSONException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 直播问答布局区域控制类
 */
public class QaLayoutController extends BaseLayoutController {

    @BindView(R.id.rv_qa_container)
    RecyclerView mQaList;
    @BindView(R.id.id_qa_input)
    EditText qaInput;
    @BindView(R.id.self_qa_invisible)
    ImageView qaVisibleStatus;
    @BindView(R.id.qa_show_tips)
    TextView qaTips;

    private LiveQaAdapter mQaAdapter;

    private Context mContext;

    private InputMethodManager mImm;

    private Handler handler = new Handler(Looper.getMainLooper());

    public QaLayoutController(Context context, View view) {
        mContext = context;
        mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        ButterKnife.bind(this, view);
    }

    public void initQaLayout() {
        mQaList.setLayoutManager(new LinearLayoutManager(mContext));
        mQaAdapter = new LiveQaAdapter(mContext);
        mQaList.setAdapter(mQaAdapter);

        mQaList.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));

        mQaList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mImm.hideSoftInputFromWindow(qaInput.getWindowToken(), 0);
                return false;
            }
        });
    }

    public void clearQaInfo() {
        mQaAdapter.resetQaInfos();
    }

    public void addQuestion(Question question) {
        mQaAdapter.addQuestion(question);
    }

    public void showQuestion(String questionId) {
        mQaAdapter.showQuestion(questionId);
    }

    public void addAnswer(Answer answer) {
        mQaAdapter.addAnswer(answer);
    }

    @OnClick(R.id.id_qa_input)
    void inputQaMsg() {

    }

    @OnClick(R.id.self_qa_invisible)
    void changeShowQaStatus() {
        if (qaVisibleStatus.isSelected()) {
            qaVisibleStatus.setSelected(false);
            qaTips.setText("显示所有回答");
            mQaAdapter.setOnlyShowSelf(false);


        } else {
            qaVisibleStatus.setSelected(true);
            qaTips.setText("只看我的回答");
            mQaAdapter.setOnlyShowSelf(true);
        }

        removeTipsHideCallBack();
        qaTips.setVisibility(View.VISIBLE);
        handler.postDelayed(tipsRunnable, 3 * 1000);
    }

    Runnable tipsRunnable = new Runnable() {
        @Override
        public void run() {
            qaTips.setVisibility(View.INVISIBLE);
        }
    };

    public void removeTipsHideCallBack() {
        handler.removeCallbacks(tipsRunnable);
    }

    @OnClick(R.id.id_qa_send)
    void sendQaMsg() {

        // 判断如果直播未开始，则告诉用户，无法提问
        if (DWLive.getInstance().getPlayStatus() == DWLive.PlayStatus.PREPARING) {
            toastOnUiThread(mContext, "直播未开始，无法提问");
            return;
        }

        // 直播中，提问判断内容是否符合要求，符合要求，进行提问
        String questionMsg = qaInput.getText().toString().trim();
        if (TextUtils.isEmpty(questionMsg)) {
            toastOnUiThread(mContext, "输入信息不能为空");
        } else {
            try {
                DWLive.getInstance().sendQuestionMsg(questionMsg);
                qaInput.setText("");
                mImm.hideSoftInputFromWindow(qaInput.getWindowToken(), 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}