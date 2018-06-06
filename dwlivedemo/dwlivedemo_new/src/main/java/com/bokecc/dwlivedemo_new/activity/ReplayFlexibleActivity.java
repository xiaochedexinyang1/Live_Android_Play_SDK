package com.bokecc.dwlivedemo_new.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.adapter.LivePublicChatAdapter;
import com.bokecc.dwlivedemo_new.adapter.LiveQaAdapter;
import com.bokecc.dwlivedemo_new.global.QaInfo;
import com.bokecc.dwlivedemo_new.module.ChatEntity;
import com.bokecc.dwlivedemo_new.util.TimeUtil;
import com.bokecc.sdk.mobile.live.pojo.Answer;
import com.bokecc.sdk.mobile.live.pojo.Question;
import com.bokecc.sdk.mobile.live.pojo.RoomInfo;
import com.bokecc.sdk.mobile.live.pojo.TemplateInfo;
import com.bokecc.sdk.mobile.live.replay.flexible.DWLiveFlexibleReplay;
import com.bokecc.sdk.mobile.live.replay.flexible.DWLiveFlexibleReplayListener;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayAnswerMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayChatMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayLoginInfo;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayQAMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayQuestionMsg;
import com.bokecc.sdk.mobile.live.widget.DocView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 灵活版在线回放SDK集成的Demo页 ———— 支持当前页面切换回放内容</br>
 *
 * 基于灵活版回放SDK核心类：DWLiveFlexibleRepla、DWLiveFlexibleReplayListener
 */
public class ReplayFlexibleActivity extends Activity implements TextureView.SurfaceTextureListener, DWLiveFlexibleReplayListener, View.OnClickListener {

    private static final String TAG = "ReplayFlexibleActivity";

    private Unbinder unbinder;

    @BindView(R.id.replay_textureview)
    TextureView replayTextureView;

    @BindView(R.id.replay_back)
    ImageView replayBack;   // "退出"按钮

    @BindView(R.id.replay_one)
    Button btnReplayOne;    // "回放1"按钮

    @BindView(R.id.replay_two)
    Button btnReplayTwo;    // "回放2"按钮

    @BindView(R.id.replay_three)
    Button btnReplayThree;  // "回放3"按钮

    @BindView(R.id.replay_player_control_layout)
    RelativeLayout rlReplayPlayControlLayout;  // 回放视频上面展示的控制层

    @BindView(R.id.replay_play_icon)
    ImageView playIcon;     // 播放按钮

    @BindView(R.id.pc_portrait_progressBar)
    ProgressBar progressBar;    // Loading 控件

    @BindView(R.id.replay_title)
    TextView tvReplayTitle;  // 回放标题

    @BindView(R.id.replay_current_time)
    TextView currentTime;      // 当前时间

    @BindView(R.id.replay_duration)
    TextView durationTextView;  // 视频总时长

    @BindView(R.id.replay_progressbar)
    SeekBar playSeekBar;    // 进度条

    @BindView(R.id.replay_full_screen)
    ImageView fullScreen;   // 全屏按钮

    @BindView(R.id.replay_speed)
    Button replaySpeed;     // 回放速度

    DWLiveFlexibleReplay dwLiveReplay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pc_replay_with_switch);

        unbinder = ButterKnife.bind(this);

        dwLiveReplay = DWLiveFlexibleReplay.getInstance();
        dwLiveReplay.setSecure(false);
        dwLiveReplay.setDWLiveFlexibleReplayListener(this);

        replayTextureView.setSurfaceTextureListener(this);

        initViews();
        initReplayButtons();

        initViewPager();
    }

    private void initViews() {

        // 初始化设置部分控件的初始状态状态
        fullScreen.setSelected(true);
        progressBar.setVisibility(View.GONE);
        rlReplayPlayControlLayout.setVisibility(View.VISIBLE);
        replaySpeed.setVisibility(View.VISIBLE);
        replaySpeed.setText("1.0x");

        replayBack.setOnClickListener(this);
        playIcon.setOnClickListener(this);
        fullScreen.setOnClickListener(this);
        playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (dwLiveReplay != null) {
                    dwLiveReplay.setSeekPosition(progress);
                }
            }
        });
    }

    // 初始化回放切换的按钮
    private void initReplayButtons() {
        btnReplayOne.setOnClickListener(this);
        btnReplayTwo.setOnClickListener(this);
        btnReplayThree.setOnClickListener(this);
        replaySpeed.setOnClickListener(this);
    }

    // 点击事件处理
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.replay_back:
                finish();
                break;

            // 回放切换
            case R.id.replay_one:
                ReplayLoginInfo replayLoginInfoOne = new ReplayLoginInfo();
                replayLoginInfoOne.setRoomId("7A69CC542B18A9AB9C33DC5901307461");
                replayLoginInfoOne.setUserId("B27039502337407C");
                replayLoginInfoOne.setLiveId("C5E179F3DA38A94A");
                replayLoginInfoOne.setRecordId("DAF45492DF286EDA");
                replayLoginInfoOne.setViewerName("111");
                replayLoginInfoOne.setViewerToken("111");
                dwLiveReplay.startPlayReplay(this, replayLoginInfoOne);
                break;
            case R.id.replay_two:
                ReplayLoginInfo replayLoginInfoTwo = new ReplayLoginInfo();
                replayLoginInfoTwo.setRoomId("1EA59D52789B12E09C33DC5901307461");
                replayLoginInfoTwo.setUserId("B27039502337407C");
                replayLoginInfoTwo.setLiveId("96A0BC19975392F6");
                replayLoginInfoTwo.setRecordId("ACD1A3EB322EF07E");
                replayLoginInfoTwo.setViewerName("111");
                replayLoginInfoTwo.setViewerToken("111");
                dwLiveReplay.startPlayReplay(this, replayLoginInfoTwo);
                break;
            case R.id.replay_three:
                ReplayLoginInfo replayLoginInfoThree = new ReplayLoginInfo();
                replayLoginInfoThree.setRoomId("080D04CB846F0FB29C33DC5901307461");
                replayLoginInfoThree.setUserId("B27039502337407C");
                replayLoginInfoThree.setLiveId("50743DD69A9B2C60");
                replayLoginInfoThree.setRecordId("3804F642D564BE78");
                replayLoginInfoThree.setViewerName("111");
                replayLoginInfoThree.setViewerToken("111");
                dwLiveReplay.startPlayReplay(this, replayLoginInfoThree);
                break;

            // 切换播放状态
            case R.id.replay_play_icon:
                changePlayerStatus();
                break;

            // 全屏与非全屏切换
            case R.id.replay_full_screen:
                if (fullScreen.isSelected()) {
                    setScreenStatus(true);
                } else {
                    setScreenStatus(false);
                }
                break;

            // 倍速切换
            case R.id.replay_speed:
                if (!TextUtils.isEmpty(replaySpeed.getText()) && replaySpeed.getText().equals("1.0x")) {
                    replaySpeed.setText("1.5x");
                    dwLiveReplay.setPlaySpeed(1.5f);
                    break;
                }

                if (!TextUtils.isEmpty(replaySpeed.getText()) && replaySpeed.getText().equals("1.5x")) {
                    replaySpeed.setText("0.5x");
                    dwLiveReplay.setPlaySpeed(0.5f);
                    break;
                }

                if (!TextUtils.isEmpty(replaySpeed.getText()) && replaySpeed.getText().equals("0.5x")) {
                    replaySpeed.setText("1.0x");
                    dwLiveReplay.setPlaySpeed(1.0f);
                    break;
                }

                break;

            default:
                break;
        }
    }

    // 设置全屏状态
    public void setScreenStatus(boolean isFull) {
        if (isFull) {
            fullScreen.setSelected(false);
            tagRadioGroup.setVisibility(View.GONE);
            rlInfoLayout.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            fullScreen.setSelected(true);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            tagRadioGroup.setVisibility(View.VISIBLE);
            rlInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    // 设置播放状态
    private void changePlayerStatus() {
        if (playIcon.isSelected()) {
            playIcon.setSelected(false);
            dwLiveReplay.changePlayerStatus(false);
        } else {
            playIcon.setSelected(true);
            dwLiveReplay.changePlayerStatus(true);
        }
    }

    //------------------------------- Activity生命周期相关 ----------------------------------


    @Override
    protected void onPause() {
        super.onPause();

        // 调用生命周期
        if (dwLiveReplay != null) {
            dwLiveReplay.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ButterKnife 解绑
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }

        // 调用生命周期
        if (dwLiveReplay != null) {
            dwLiveReplay.onDestroy();
        }
    }


    //------------------------------- Surface生命周期相关 ----------------------------------

    Surface surface;

    /**
     * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
     *
     * @param surfaceTexture The surface returned by
     *                       {@link TextureView#getSurfaceTexture()}
     * @param width          The width of the surface
     * @param height         The height of the surface
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        this.surface = new Surface(surfaceTexture);
        dwLiveReplay.setSurface(this.surface);
    }

    /**
     * Invoked when the {@link SurfaceTexture}'s buffers size changed.
     *
     * @param surface The surface returned by
     *                {@link TextureView#getSurfaceTexture()}
     * @param width   The new width of the surface
     * @param height  The new height of the surface
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    /**
     * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
     * If returns true, no rendering should happen inside the surface texture after this method
     * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
     * Most applications should return true.
     *
     * @param surface The surface about to be destroyed
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.surface = null;
        return false;
    }

    /**
     * Invoked when the specified {@link SurfaceTexture} is updated through
     * {@link SurfaceTexture#updateTexImage()}.
     *
     * @param surface The surface just updated
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    // ------------------------------ 回放SDK 的回调方法 ------------------------------

    /**
     * 直播间登录成功回调方法
     *
     * @param roomInfo     当前直播间信息
     * @param templateInfo 当前直播间模板信息
     */
    @Override
    public void onLogin(final RoomInfo roomInfo, final TemplateInfo templateInfo) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 设置直播间标题
                tvReplayTitle.setText(Html.fromHtml(roomInfo.getName()));
            }
        });
    }

    /**
     * 回放直播间登录失败
     *
     * @param reason 失败原因
     */
    @Override
    public void onLoginFailed(String reason) {
        Log.e(TAG, reason);
    }

    /**
     * 播放器处于"已准备"状态<br/>
     * 对应Ijk播放器回调OnPreparedListener.onPrepared(IMediaPlayer mp)
     */
    @Override
    public void onPrepared() {
        Log.e(TAG, "onPrepared");
    }

    /**
     * 开始缓冲
     */
    @Override
    public void onBufferStart() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * 缓冲结束
     */
    @Override
    public void onBufferEnd() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * 回调视频总时长
     *
     * @param totalTime 视频总时长
     */
    @Override
    public void onDuration(final long totalTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                durationTextView.setText(TimeUtil.getFormatTime(totalTime));
                playSeekBar.setMax((int) totalTime);
            }
        });
    }

    /**
     * 回调当前播放的时间
     *
     * @param nowTime 当前播放时间
     */
    @Override
    public void onPlayTime(final long nowTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentTime.setText(TimeUtil.getFormatTime(nowTime));
                playSeekBar.setProgress((int) nowTime);
            }
        });
    }

    /**
     * 回调播放器状态改变
     *
     * @param isPlaying 当前是否在播放
     */
    @Override
    public void onPlayStatusChange(final boolean isPlaying) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    playIcon.setSelected(true);
                } else {
                    playIcon.setSelected(false);
                }
            }
        });
    }

    /**
     * 当前回放播放结束
     */
    @Override
    public void onCompletion() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ReplayFlexibleActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
            }
        });
    }

    ArrayList<ChatEntity> mChatEntities = new ArrayList<>();

    /**
     * 回调所有的聊天信息
     *
     * @param replayChatMsgs 聊天信息
     */
    @Override
    public void onChatMessage(TreeSet<ReplayChatMsg> replayChatMsgs) {
        ArrayList<ChatEntity> chatEntities = new ArrayList<>();

        for (ReplayChatMsg msg : replayChatMsgs) {
            chatEntities.add(getReplayChatEntity(msg));
        }

        mChatEntities = chatEntities;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chatLayoutController != null) {
                    chatLayoutController.addChatEntities(mChatEntities);
                }
            }
        });
    }

    private LinkedHashMap<String, QaInfo> mQaInfoMap;

    /**
     * 回调所有的问答数据
     *
     * @param qaMsgs 问答数据
     */
    @Override
    public void onQuestionAnswer(TreeSet<ReplayQAMsg> qaMsgs) {
        LinkedHashMap<String, QaInfo> qaInfoMap = new LinkedHashMap<>();

        for (ReplayQAMsg qaMsg : qaMsgs) {

            ReplayQuestionMsg questionMsg = qaMsg.getReplayQuestionMsg();
            Question question = new Question();
            question.setContent(questionMsg.getContent())
                    .setId(questionMsg.getQuestionId())
                    .setQuestionUserId(questionMsg.getQuestionUserId())
                    .setQuestionUserName(questionMsg.getQuestionUserName())
                    .setTime(String.valueOf(questionMsg.getTime()))
                    .setUserAvatar(questionMsg.getQuestionUserAvatar());

            TreeSet<ReplayAnswerMsg> answerMsgs = qaMsg.getReplayAnswerMsgs();

            // 没有回答
            if (answerMsgs.size() < 1) {
                if (questionMsg.getIsPublish() == 0) {
                    // 未发布的问题
                    continue;
                } else if (questionMsg.getIsPublish() == 1) {
                    // 发布的问题
                    QaInfo qaInfo = new QaInfo(question);
                    qaInfoMap.put(question.getId(), qaInfo);
                    continue;
                }
            }

            // 回答过
            QaInfo qaInfo = new QaInfo(question);
            for (ReplayAnswerMsg answerMsg : answerMsgs) {
                Answer answer = new Answer();
                answer.setUserAvatar(answerMsg.getUserAvatar())
                        .setContent(answerMsg.getContent())
                        .setAnswerUserId(answerMsg.getUserId())
                        .setAnswerUserName(answerMsg.getUserName())
                        .setReceiveTime(String.valueOf(answerMsg.getTime()))
                        .setUserRole(answerMsg.getUserRole());
                qaInfo.addAnswer(answer);
            }

            qaInfoMap.put(question.getId(), qaInfo);
        }

        mQaInfoMap = qaInfoMap;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                qaLayoutController.addReplayQAInfos(mQaInfoMap);
            }
        });
    }

    private ChatEntity getReplayChatEntity(ReplayChatMsg msg) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(msg.getUserId());
        chatEntity.setUserName(msg.getUserName());
        chatEntity.setPrivate(false);
        chatEntity.setPublisher(true);
        chatEntity.setMsg(msg.getContent());
        chatEntity.setTime(String.valueOf(msg.getTime()));
        chatEntity.setUserAvatar(msg.getAvatar());
        return chatEntity;
    }


    // ------------------------------- 视频下方布局：聊天、问答、文档等 -------------------------------

    private DocView docView;

    @BindView(R.id.pc_live_infos_layout)
    RelativeLayout rlInfoLayout;

    @BindView(R.id.rg_infos_tag)
    RadioGroup tagRadioGroup;

    @BindView(R.id.live_portrait_info_document)
    RadioButton docTag;

    @BindView(R.id.live_portrait_info_chat)
    RadioButton chatTag;

    @BindView(R.id.live_portrait_info_qa)
    RadioButton qaTag;

    @BindView(R.id.live_portrait_container_viewpager)
    ViewPager infoLayoutContainer;

    List<View> infoList = new ArrayList<>();
    List<Integer> tagIdList = new ArrayList<>();
    List<RadioButton> tagRBList = new ArrayList<>();

    View docLayout;
    View chatLayout;
    View qaLayout;

    PagerAdapter adapter;

    DocLayoutController docLayoutController;
    ChatLayoutController chatLayoutController;
    QaLayoutController qaLayoutController;

    private void initViewPager() {

        LayoutInflater inflater = LayoutInflater.from(this);

        //  初始化文档区域
        initDocLayout(inflater);
        docView = docLayoutController.getDocView();
        dwLiveReplay.setDocView(docView);
        rlInfoLayout.setVisibility(View.VISIBLE);

        // 初始化聊天区域
        initChatLayout(inflater);

        // 初始化问答区域
        initQaLayout(inflater);

        adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return infoList.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(infoList.get(position));
                return infoList.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(infoList.get(position));
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };

        infoLayoutContainer.setAdapter(adapter);

        infoLayoutContainer.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tagRBList.get(position).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tagRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                infoLayoutContainer.setCurrentItem(tagIdList.indexOf(i), true);
            }
        });


        if (tagRBList.contains(chatTag)) {
            chatTag.performClick();
        } else if (tagRBList.size() > 0) {
            tagRBList.get(0).performClick();
        }

    }

    //----------------------文档----------------------------

    class DocLayoutController {

        @BindView(R.id.live_doc)
        DocView mDocView;

        Context mContext;

        DocLayoutController(Context context, View view) {
            mContext = context;
            ButterKnife.bind(this, view);
        }

        DocView getDocView() {
            return mDocView;
        }
    }

    // 初始化文档区域
    private void initDocLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_document);
        tagRBList.add(docTag);
        docTag.setVisibility(View.VISIBLE);
        docLayout = inflater.inflate(R.layout.live_portrait_doc_layout, null);
        infoList.add(docLayout);
        docLayoutController = new DocLayoutController(this, docLayout);
    }


    //----------------------聊天-----------------------------
    public class ChatLayoutController {

        @BindView(R.id.chat_container)
        RecyclerView mChatList;

        @BindView(R.id.iv_live_pc_private_chat)
        ImageView mPrivateChatIcon;

        @BindView(R.id.id_private_chat_user_layout)
        LinearLayout mPrivateChatUserLayout;

        @BindView(R.id.id_push_chat_layout)
        RelativeLayout mChatLayout;

        int mChatInfoLength;

        Context mContext;

        ChatLayoutController(Context context, View view) {
            mContext = context;
            ButterKnife.bind(this, view);
            mChatInfoLength = 0;
            mChatLayout.setVisibility(View.GONE);
            mPrivateChatIcon.setVisibility(View.GONE);
        }

        LivePublicChatAdapter mChatAdapter;

        void initChat() {
            mChatList.setLayoutManager(new LinearLayoutManager(mContext));
            mChatAdapter = new LivePublicChatAdapter(mContext);
            mChatList.setAdapter(mChatAdapter);
        }

        /**
         * 回放的聊天添加
         *
         * @param chatEntities
         */
        public void addChatEntities(ArrayList<ChatEntity> chatEntities) {
            // 回放的聊天内容随时间轴推进展示
            if (DWApplication.REPLAY_CHAT_FOLLOW_TIME) {
                // 如果数据长度没发生变化就不刷新
                if (mChatInfoLength != chatEntities.size()) {
                    mChatAdapter.add(chatEntities);
                    mChatList.scrollToPosition(chatEntities.size() - 1);
                    mChatInfoLength = chatEntities.size();
                }
            } else {
                mChatAdapter.add(chatEntities);
            }
        }
    }

    private void initChatLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_chat);
        tagRBList.add(chatTag);
        chatTag.setVisibility(View.VISIBLE);
        chatLayout = inflater.inflate(R.layout.live_portrait_chat_layout, null);
        infoList.add(chatLayout);

        chatLayoutController = new ChatLayoutController(this, chatLayout);
        chatLayoutController.initChat();

    }

    //----------------------问答----------------------------
    class QaLayoutController {

        @BindView(R.id.rv_qa_container)
        RecyclerView mQaList;

        @BindView(R.id.rl_qa_input_layout)
        RelativeLayout mInputLayout;

        LiveQaAdapter mQaAdapter;

        int mQaInfoLength;

        Context mContext;

        QaLayoutController(Context context, View view) {
            mContext = context;
            ButterKnife.bind(this, view);
            mQaInfoLength = 0;
            mInputLayout.setVisibility(View.GONE);
        }

        void initQaLayout() {
            mQaList.setLayoutManager(new LinearLayoutManager(mContext));
            mQaAdapter = new LiveQaAdapter(mContext);
            mQaList.setAdapter(mQaAdapter);
        }

        public void addReplayQAInfos(LinkedHashMap<String, QaInfo> replayQaInfos) {
            mQaAdapter.addReplayQuestoinAnswer(replayQaInfos);
        }
    }

    private void initQaLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_qa);
        tagRBList.add(qaTag);
        qaTag.setVisibility(View.VISIBLE);
        qaLayout = inflater.inflate(R.layout.live_portrait_qa_layout, null);
        infoList.add(qaLayout);

        qaLayoutController = new QaLayoutController(this, qaLayout);
        qaLayoutController.initQaLayout();
    }

}
