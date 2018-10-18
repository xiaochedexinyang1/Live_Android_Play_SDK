package com.bokecc.dwlivedemo_new.activity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.base.BaseActivity;
import com.bokecc.dwlivedemo_new.controller.replay.ChatLayoutController;
import com.bokecc.dwlivedemo_new.controller.replay.DocLayoutController;
import com.bokecc.dwlivedemo_new.controller.replay.QaLayoutController;
import com.bokecc.dwlivedemo_new.global.QaInfo;
import com.bokecc.dwlivedemo_new.manage.ReplayPlayerManager;
import com.bokecc.dwlivedemo_new.module.ChatEntity;
import com.bokecc.dwlivedemo_new.popup.CommonPopup;
import com.bokecc.sdk.mobile.live.Exception.DWLiveException;
import com.bokecc.sdk.mobile.live.pojo.Answer;
import com.bokecc.sdk.mobile.live.pojo.Question;
import com.bokecc.sdk.mobile.live.replay.DWLiveReplay;
import com.bokecc.sdk.mobile.live.replay.DWLiveReplayListener;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayAnswerMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayBroadCastMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayChatMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayPageInfo;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayQAMsg;
import com.bokecc.sdk.mobile.live.replay.pojo.ReplayQuestionMsg;
import com.bokecc.sdk.mobile.live.widget.DocView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.OnClick;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * CC 在线回放 页面
 */
public class ReplayActivity extends BaseActivity implements TextureView.SurfaceTextureListener,
        IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnInfoListener,
        IMediaPlayer.OnVideoSizeChangedListener,
        IMediaPlayer.OnCompletionListener {

    private final static String TAG = "ReplayActivity";

    @BindView(R.id.rl_pc_live_top_layout)
    RelativeLayout rlLiveTopLayout;

    @BindView(R.id.textureview_pc_live_play)
    TextureView mPlayerContainer;

    @BindView(R.id.replay_player_control_layout)
    RelativeLayout playerControlLayout;

    @BindView(R.id.pc_live_infos_layout)
    RelativeLayout rlLiveInfosLayout;

    @BindView(R.id.pc_portrait_progressBar)
    ProgressBar pcPortraitProgressBar;

    ReplayPlayerManager replayPlayerManager;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        replayPlayerManager.setScreenVisible(true, true);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setPortraitLayoutVisibility(View.VISIBLE);
            rlLiveTopLayout.setVisibility(View.VISIBLE);
            tagRadioGroup.setVisibility(View.VISIBLE);
            playerControlLayout.setVisibility(View.VISIBLE);
            mPlayerContainer.setLayoutParams(getVideoSizeParams());
            replayPlayerManager.onConfiChanged(true);
            if (inDocFullMode) {
                dwLiveReplay.docApplyNewConfig(newConfig);
                inDocFullMode = false;
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (toDocFullMode) {
                rlLiveTopLayout.setVisibility(View.GONE);
                tagRadioGroup.setVisibility(View.GONE);
                dwLiveReplay.docApplyNewConfig(newConfig);
                toDocFullMode = false;
                inDocFullMode = true;
            } else {
                setPortraitLayoutVisibility(View.GONE);
                playerControlLayout.setVisibility(View.VISIBLE);
                mPlayerContainer.setLayoutParams(getVideoSizeParams());
                replayPlayerManager.onConfiChanged(false);
            }
        }
    }


    @OnClick(R.id.rl_pc_live_top_layout)
    void onPlayOnClick(View v) {
        boolean isLayoutShown = replayPlayerManager.OnPlayClick();
    }

    private View mRoot;
    private IjkMediaPlayer player;
    private DWLiveReplay dwLiveReplay = DWLiveReplay.getInstance();

    private WindowManager wm;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pc_replay;
    }


    @Override
    protected void onViewCreated() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mRoot = getWindow().getDecorView().findViewById(android.R.id.content);
        // 屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }

        replayPlayerManager = new ReplayPlayerManager(this, playerControlLayout, mRoot);
        replayPlayerManager.init();

        initClosePopup();

        initViewPager();

        initPlayer();

    }


    //-------------------------- CC SDK 回放视频 生命周期相关 Start -------------------------------

    /** isOnResumeStart 的意义在于部分手机从Home跳回到APP的时候，不会触发onSurfaceTextureAvailable */
    boolean isOnResumeStart = false;

    @Override
    protected void onResume() {
        super.onResume();
        // 判断是否在文档全屏模式下，如果在，就退出全屏模式，触发重新拉流的操作
        if (inDocFullMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        isOnResumeStart = false;
        if (surface != null) {
            dwLiveReplay.start(surface);
            isOnResumeStart = true;
        }
    }


    boolean isOnPause = false;

    long currentPosition;

    @Override
    protected void onPause() {
        super.onPause();
        isPrepared = false;
        isOnPause = true;
        if (player != null) {
            player.pause();
            if (player.getCurrentPosition() != 0) {
                currentPosition = player.getCurrentPosition();
            }
        }

        if (qaLayoutController != null) {
            qaLayoutController.clearQaInfo();
        }

        dwLiveReplay.stop();
        stopTimerTask();
        stopNetworkTimer();
    }


    @Override
    protected void onDestroy() {
        replayPlayerManager.onDestroy();

        if (timerTask != null) {
            timerTask.cancel();
        }

        if (player != null) {
            player.pause();
            player.stop();
            player.release();
        }

        dwLiveReplay.onDestroy();

        super.onDestroy();
    }

    Surface surface;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surface = new Surface(surfaceTexture);
        if (isOnResumeStart) {
            return;
        }
        dwLiveReplay.start(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        surface = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }


    //-------------------------- CC SDK 回放视频 生命周期相关 END -------------------------------


    //-------------------------- CC SDK 回放视频 播放器相关 Start -----------------------------

    /** 初始化播放器 */
    private void initPlayer() {
        mPlayerContainer.setSurfaceTextureListener(this);
        player = new IjkMediaPlayer();
        player.setOnPreparedListener(this);
        player.setOnInfoListener(this);
        player.setOnVideoSizeChangedListener(this);
        player.setOnCompletionListener(this);

        player.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                replayPlayerManager.setBufferPercent(percent);
            }
        });

        dwLiveReplay.setReplayParams(myDWLiveReplayListener, this, player, docView);
    }

    boolean isPrepared = false;

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        isPrepared = true;
        player.start();

        if (currentPosition > 0) {
            player.seekTo(currentPosition);
        }

        pcPortraitProgressBar.setVisibility(View.GONE);
        playerControlLayout.setVisibility(View.VISIBLE);

        if (isPortrait()) {
            setPortraitLayoutVisibility(View.VISIBLE);
        } else {
            setPortraitLayoutVisibility(View.GONE);
        }

        if (replayPlayerManager != null) {
            replayPlayerManager.onPrepared();
            replayPlayerManager.setDurationTextView(player.getDuration());
        }

        // 更新一下当前播放的按钮的状态
        replayPlayerManager.changePlayIconStatus(player.isPlaying());

        startTimerTask();

        isNetworkConnected = true;
        startNetworkTimer();

    }

    Runnable r;
    Handler handler = new Handler(Looper.getMainLooper());
    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int arg1, int i1) {

        if (arg1 == IMediaPlayer.MEDIA_INFO_BUFFERING_START) { // 开始缓冲
            r = new Runnable() {
                @Override
                public void run() {
                    if (qaLayoutController != null) {
                        qaLayoutController.clearQaInfo();
                    }
                    dwLiveReplay.stop();
                    dwLiveReplay.start(surface);
                }
            };

            handler.postDelayed(r, 10 * 1000); // 延时定时器，此处设置的是10s，可自行设置
        } else if(arg1 == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (r != null) {
                handler.removeCallbacks(r); // 如果收到了缓冲结束，那么取消延时定时器
            }
        }

        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {

        if (width == 0 || height == 0) {
            return;
        }
        mPlayerContainer.setLayoutParams(getVideoSizeParams());
    }

    // 视频等比缩放
    private RelativeLayout.LayoutParams getVideoSizeParams() {

        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        int vWidth = player.getVideoWidth();
        int vHeight = player.getVideoHeight();

        if(isPortrait()) {
            height = height / 3; //TODO 根据当前布局更改
        }

        if (vWidth == 0) {
            vWidth = 600;
        }
        if (vHeight == 0) {
            vHeight = 400;
        }

        if (vWidth > width || vHeight > height) {
            float wRatio = (float) vWidth / (float) width;
            float hRatio = (float) vHeight / (float) height;
            float ratio = Math.max(wRatio, hRatio);

            width = (int) Math.ceil((float) vWidth / ratio);
            height = (int) Math.ceil((float) vHeight / ratio);
        } else {
            float wRatio = (float) width / (float) vWidth;
            float hRatio = (float) height / (float) vHeight;
            float ratio = Math.min(wRatio, hRatio);

            width = (int) Math.ceil((float) vWidth * ratio);
            height = (int) Math.ceil((float) vHeight * ratio);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        return params;
    }


    boolean isComplete = false;

    /** 播放结束，会回调此方法 */
    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        isComplete = true;
    }

    /** 设置播放跳转的的位置 */
    public void setSeekPosition(int position) {
        player.seekTo(position);
        if (isComplete) {
            player.start();
            isComplete = false;
            replayPlayerManager.setPlayingStatusIcon();
        }
    }

    /** 设置播放状态（由界面控件调用）*/
    public void setPlayerStatus(boolean isPlaying) {
        if (isPlaying) {
            player.start();
        } else {
            player.pause();
        }
    }

    //-------------------------- CC SDK 回放视频 播放器相关 END -----------------------------


    private ArrayList<ChatEntity> mChatEntities;
    private LinkedHashMap<String, QaInfo> mQaInfoMap;

    private ChatEntity getReplayChatEntity(ReplayChatMsg msg) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(msg.getUserId());
        chatEntity.setUserName(msg.getUserName());
        chatEntity.setPrivate(false);
        chatEntity.setPublisher(true);
        chatEntity.setMsg(msg.getContent());
        chatEntity.setTime(String.valueOf(msg.getTime())); // TODO 看看到底是个啥
        chatEntity.setUserAvatar(msg.getAvatar());
        return chatEntity;
    }

    // 直播回放回调监听
    private DWLiveReplayListener myDWLiveReplayListener = new DWLiveReplayListener() {

        @Override
        public void onQuestionAnswer(TreeSet<ReplayQAMsg> qaMsgs) {

            LinkedHashMap<String, QaInfo> qaInfoMap = new LinkedHashMap<>();

            for (ReplayQAMsg qaMsg: qaMsgs) {

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
                for (ReplayAnswerMsg answerMsg:answerMsgs) {
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
                    // 如果不需要根据时间轴展示，就直接交给UI去做展示
                    if (!DWApplication.REPLAY_QA_FOLLOW_TIME) {
                        if (qaLayoutController != null) {
                            qaLayoutController.addReplayQAInfos(mQaInfoMap);
                        }
                    }
                }
            });
        }

        @Override
        public void onChatMessage(TreeSet<ReplayChatMsg> replayChatMsgs) {

            ArrayList<ChatEntity> chatEntities = new ArrayList<>();

            for (ReplayChatMsg msg: replayChatMsgs) {
                chatEntities.add(getReplayChatEntity(msg));
            }

            mChatEntities = chatEntities;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 如果不需要根据时间轴展示，就直接交给UI去做展示
                    if (!DWApplication.REPLAY_CHAT_FOLLOW_TIME) {
                        if (chatLayoutController != null) {
                            chatLayoutController.addChatEntities(mChatEntities);
                        }
                    }
                }
            });
        }

        /**
         * 广播信息
         *
         * @param broadCastMsgList 广播信息列表
         */
        @Override
        public void onBroadCastMessage(ArrayList<ReplayBroadCastMsg> broadCastMsgList) {
            if (broadCastMsgList == null) {
                return;
            }

            if (broadCastMsgList.size() > 0) {
                for (ReplayBroadCastMsg broadCastMsg : broadCastMsgList) {
                    Log.i(TAG, "广播内容 ：" + broadCastMsg.getContent() + ", 发布时间：" + broadCastMsg.getTime());
                }
            }
        }

        @Override
        public void onPageInfoList(ArrayList<ReplayPageInfo> infoList) {
            // TODO 回放页面信息列表
        }

        /**
         * 回调当前翻页的信息<br/>
         * 注意：<br/>
         * 白板docTotalPage一直为0，pageNum从1开始<br/>
         * 其他文档docTotalPage为正常页数，pageNum从0开始
         *
         * @param docId        文档Id
         * @param docName      文档名称
         * @param pageNum      当前页码
         * @param docTotalPage 当前文档总共的页数
         */
        @Override
        public void onPageChange(String docId, String docName, int pageNum, int docTotalPage) {
            Log.i(TAG, "文档ID ：" + docId + ", 文档名称：" + docName + ", 当前页码：" + pageNum + ", 总共页数：" + docTotalPage);
        }

        @Override
        public void onException(DWLiveException exception) {

        }

        @Override
        public void onInitFinished() {

        }
    };


    // --------------------- Demo 随时间展示聊天、问答任务  --------------------

    Timer timer = new Timer();
    TimerTask timerTask;

    private void startTimerTask() {

        stopTimerTask();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!player.isPlaying() && (player.getDuration() - player.getCurrentPosition() < 500)) {
                    replayPlayerManager.setCurrentTime(player.getDuration());
                } else {
                    replayPlayerManager.setCurrentTime(player.getCurrentPosition());
                }

                // 回放的聊天内容随时间轴推进展示
                if (DWApplication.REPLAY_CHAT_FOLLOW_TIME) {
                    if (mChatEntities != null && mChatEntities.size() >= 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<ChatEntity> temp_chatEntities = new ArrayList<>();
                                int time = Math.round(player.getCurrentPosition() / 1000);
                                for (ChatEntity entity : mChatEntities) {
                                    if (!TextUtils.isEmpty(entity.getTime()) && time >= Integer.valueOf(entity.getTime())) {
                                        temp_chatEntities.add(entity);
                                    }
                                }
                                if (chatLayoutController != null) {
                                    chatLayoutController.addChatEntities(temp_chatEntities);
                                }

                            }
                        });
                    }
                }

                // 回放的问答内容随时间轴推进展示
                if (DWApplication.REPLAY_QA_FOLLOW_TIME) {
                    if (mQaInfoMap != null && mQaInfoMap.size() >= 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LinkedHashMap<String, QaInfo> temp_qaInfoMap = new LinkedHashMap<>();
                                int time = Math.round(player.getCurrentPosition() / 1000);
                                Iterator it = mQaInfoMap.entrySet().iterator();
                                while(it.hasNext()) {
                                    Map.Entry entity = (Map.Entry) it.next();
                                    QaInfo qaInfo = (QaInfo) entity.getValue();
                                    String key = entity.getKey().toString();
                                    if (!TextUtils.isEmpty(qaInfo.getQuestion().getTime()) && time >= Integer.valueOf(qaInfo.getQuestion().getTime())) {
                                        temp_qaInfoMap.put(key, qaInfo);
                                    }
                                }
                                if (qaLayoutController != null) {
                                    qaLayoutController.addReplayQAInfos(temp_qaInfoMap);
                                }
                            }
                        });
                    }
                }
            }
        };
        timer.schedule(timerTask, 0, 1 * 1000);
    }

    private void stopTimerTask() {
        if (timerTask != null) {
            timerTask.cancel();
        }
    }


    // --------------------- Demo 网络监测时间任务  --------------------

    boolean isNetworkConnected;

    private Timer timerNetwork = new Timer();

    private TimerTask timerTaskNetwork;

    private void startNetworkTimer() {

        if (timerTaskNetwork != null) {
            timerTaskNetwork.cancel();
        }

        timerTaskNetwork = new TimerTask() {

            @Override
            public void run() {
                if (isNetworkConnected()) {
                    if (isNetworkConnected) {
                        return;
                    } else {
                        currentPosition = player.getCurrentPosition();
                        if (qaLayoutController != null) {
                            qaLayoutController.clearQaInfo();
                        }
                        dwLiveReplay.stop();
                        dwLiveReplay.start(surface);

                    }
                    isNetworkConnected = true;
                } else {
                    if (!isNetworkConnected) {
                        return;
                    }
                    isNetworkConnected = false;
                }
            }
        };

        timerNetwork.schedule(timerTaskNetwork, 0, 1 * 1000);
    }

    private void stopNetworkTimer() {
        if (timerTaskNetwork != null) {
            timerTaskNetwork.cancel();
        }
    }

    // --------------------- Demo 视频下方布局 文档、问答、聊天布局 Start --------------------

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

    DocLayoutController docLayoutController;
    ChatLayoutController chatLayoutController;
    QaLayoutController qaLayoutController;

    private final static String VIEW_VISIBLE_TAG = "1";

    private boolean toDocFullMode;  // 是否要进入文档全屏模式
    private boolean inDocFullMode;  // 当前是否在文档全屏模式
    private DocView docView;

    // 双击全屏相关
    boolean isMove = false;
    private final static int DOUBLE_TAP_TIMEOUT = 200;
    private MotionEvent mPreviousUpEvent;

    // 初始化下方布局的ViewPager
    private void initViewPager() {

        LayoutInflater inflater = LayoutInflater.from(this);

        if (VIEW_VISIBLE_TAG.equals(dwLiveReplay.getTemplateInfo().getPdfView())) {
            initDocLayout(inflater);
            docView = docLayoutController.getDocView();
            docView.setClickable(true); // 设置文档区域可点击

            // 设置触摸监听，判断双击事件
            docView.setTouchEventListener(new DocView.TouchEventListener() {
                @Override
                public void onTouchEvent(MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        isMove = true;
                    }
                    else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (mPreviousUpEvent != null && isConsideredDoubleTap(mPreviousUpEvent, event)) {
                            if (isPortrait()) {
                                // 进入文档全屏
                                toDocFullMode = true;
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            } else {
                                // 退出文档全屏
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            }
                        }
                    }else if (event.getAction() == MotionEvent.ACTION_UP){
                        mPreviousUpEvent = MotionEvent.obtain(event);
                        isMove = false;
                    }
                }
            });

        }

        if (VIEW_VISIBLE_TAG.equals(dwLiveReplay.getTemplateInfo().getChatView())) {
            initChatLayout(inflater);
        }

        if (VIEW_VISIBLE_TAG.equals(dwLiveReplay.getTemplateInfo().getQaView())) {
            initQaLayout(inflater);
        }

        PagerAdapter adapter = new PagerAdapter() {
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

    // 初始化文档布局区域
    private void initDocLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_document);
        tagRBList.add(docTag);
        docTag.setVisibility(View.VISIBLE);
        docLayout = inflater.inflate(R.layout.live_portrait_doc_layout, null);
        infoList.add(docLayout);

        docLayoutController = new DocLayoutController(this, docLayout);
    }

    // 初始化聊天布局区域
    private void initChatLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_chat);
        tagRBList.add(chatTag);
        chatTag.setVisibility(View.VISIBLE);
        chatLayout = inflater.inflate(R.layout.live_portrait_chat_layout, null);
        infoList.add(chatLayout);

        chatLayoutController = new ChatLayoutController(this, chatLayout);
        chatLayoutController.initChat();
    }

    // 初始化问答布局区域
    private void initQaLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_qa);
        tagRBList.add(qaTag);
        qaTag.setVisibility(View.VISIBLE);
        qaLayout = inflater.inflate(R.layout.live_portrait_qa_layout, null);
        infoList.add(qaLayout);

        qaLayoutController = new QaLayoutController(this, qaLayout);
        qaLayoutController.initQaLayout();
    }

    // --------------------- Demo 视频下方布局 文档、问答、聊天 END --------------------

    //-------------------------- Demo 退出回放观看页逻辑 --------------------------

    // 退出界面弹出框
    private CommonPopup mExitPopup;

    /** 初始化 关闭回放界面 弹出框 */
    private void initClosePopup() {
        mExitPopup = new CommonPopup(this);
        mExitPopup.setOutsideCancel(true);
        mExitPopup.setKeyBackCancel(true);
        mExitPopup.setTip("您确认结束观看吗?");
        mExitPopup.setOKClickListener(new CommonPopup.OnOKClickListener() {
            @Override
            public void onClick() {
                finish();
            }
        });
    }

    // Back 键相关逻辑处理
    @Override
    public void onBackPressed() {
        if (!isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        } else {
            if (chatLayoutController != null && chatLayoutController.onBackPressed()) {
                return;
            }
        }

        mExitPopup.show(mRoot);
    }

    //-------------------------- Demo 回放观看页 工具方法 -------------------------------

    // 判断当前屏幕朝向是否为竖屏
    public boolean isPortrait() {
        int mOrientation = getApplicationContext().getResources().getConfiguration().orientation;
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        } else {
            return true;
        }
    }

    // 检测网络是否可用
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isAvailable();
    }

    // 检测是否是双击事件
    private boolean isConsideredDoubleTap(MotionEvent firstUp, MotionEvent secondDown){
        if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {
            return false;
        }
        int deltaX =(int) firstUp.getX() - (int)secondDown.getX();
        int deltaY =(int) firstUp.getY()- (int)secondDown.getY();
        return deltaX * deltaX + deltaY * deltaY < 10000;
    }

    // 设置屏幕朝向 -- 视频全屏功能调用
    public void setScreenStatus(boolean isFull) {
        if (isFull) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    // 设置竖屏布局内容是否展示
    private void setPortraitLayoutVisibility(int i) {
        rlLiveInfosLayout.setVisibility(i);
    }

}