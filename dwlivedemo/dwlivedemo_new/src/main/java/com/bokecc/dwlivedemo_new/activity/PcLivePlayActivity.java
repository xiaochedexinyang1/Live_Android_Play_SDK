package com.bokecc.dwlivedemo_new.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.base.BaseActivity;
import com.bokecc.dwlivedemo_new.controller.live.ChatLayoutController;
import com.bokecc.dwlivedemo_new.controller.live.DocLayoutController;
import com.bokecc.dwlivedemo_new.controller.live.IntroLayoutController;
import com.bokecc.dwlivedemo_new.controller.live.QaLayoutController;
import com.bokecc.dwlivedemo_new.manage.AppRTCAudioManager;
import com.bokecc.dwlivedemo_new.manage.PcLiveLandscapeViewManager;
import com.bokecc.dwlivedemo_new.manage.PcLivePortraitViewManager;
import com.bokecc.dwlivedemo_new.module.ChatEntity;
import com.bokecc.dwlivedemo_new.popup.CommonPopup;
import com.bokecc.dwlivedemo_new.popup.ExeternalQuestionnairePopup;
import com.bokecc.dwlivedemo_new.popup.LotteryPopup;
import com.bokecc.dwlivedemo_new.popup.LotteryStartPopup;
import com.bokecc.dwlivedemo_new.popup.QuestionnairePopup;
import com.bokecc.dwlivedemo_new.popup.QuestionnaireStatisPopup;
import com.bokecc.dwlivedemo_new.popup.QuestionnaireStopPopup;
import com.bokecc.dwlivedemo_new.popup.RollCallPopup;
import com.bokecc.dwlivedemo_new.popup.RtcPopup;
import com.bokecc.dwlivedemo_new.popup.VotePopup;
import com.bokecc.dwlivedemo_new.util.SoftKeyBoardState;
import com.bokecc.dwlivedemo_new.view.BarrageLayout;
import com.bokecc.dwlivedemo_new.view.LiveFloatingView;
import com.bokecc.sdk.mobile.live.DWLive;
import com.bokecc.sdk.mobile.live.DWLiveListener;
import com.bokecc.sdk.mobile.live.DWLivePlayer;
import com.bokecc.sdk.mobile.live.Exception.DWLiveException;
import com.bokecc.sdk.mobile.live.pojo.Answer;
import com.bokecc.sdk.mobile.live.pojo.BroadCastMsg;
import com.bokecc.sdk.mobile.live.pojo.ChatMessage;
import com.bokecc.sdk.mobile.live.pojo.PrivateChatInfo;
import com.bokecc.sdk.mobile.live.pojo.QualityInfo;
import com.bokecc.sdk.mobile.live.pojo.Question;
import com.bokecc.sdk.mobile.live.pojo.QuestionnaireInfo;
import com.bokecc.sdk.mobile.live.pojo.QuestionnaireStatisInfo;
import com.bokecc.sdk.mobile.live.pojo.RoomDocInfo;
import com.bokecc.sdk.mobile.live.pojo.TemplateInfo;
import com.bokecc.sdk.mobile.live.rtc.RtcClient;
import com.bokecc.sdk.mobile.live.widget.DocView;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * CC 直播观看 界面
 */
public class PcLivePlayActivity extends BaseActivity implements TextureView.SurfaceTextureListener,
        IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnVideoSizeChangedListener {

    private static final String TAG = "PcLivePlayActivity";

    @BindView(R.id.pc_live_main)
    LinearLayout pc_live_main;

    @BindView(R.id.textureview_pc_live_play)
    TextureView mPlayerContainer;

    @BindView(R.id.bl_pc_barrage)
    BarrageLayout blPcBarrage;

    @BindView(R.id.rl_pc_live_top_layout)
    RelativeLayout rlLiveTopLayout;

    @BindView(R.id.rl_pc_landscape_layout)
    RelativeLayout rlLandscapeLayout;

    @BindView(R.id.rl_pc_portrait_layout)
    RelativeLayout rlPortraitLayout;

    @BindView(R.id.rl_sound_layout)
    RelativeLayout rlSoundLayout;

    @BindView(R.id.pc_live_infos_layout)
    RelativeLayout rlLiveInfosLayout;

    @BindView(R.id.tv_living)
    TextView livingSign;

    // 连麦使用的布局
    @BindView(R.id.svr_local_render)
    SurfaceViewRenderer localRender;

    @BindView(R.id.svr_remote_render)
    SurfaceViewRenderer remoteRender;

    @BindView(R.id.tv_pc_portrait_prepare)
    TextView tvPcPortraitStatusTips;

    @BindView(R.id.pc_portrait_progressBar)
    ProgressBar pcPortraitProgressBar;

    // 直播视频悬浮窗
    LiveFloatingView floatingView;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 进入竖屏视频模式
            rlLiveTopLayout.setVisibility(View.VISIBLE);
            tagRadioGroup.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setPortraitLayoutVisibility(View.VISIBLE);
            rlLandscapeLayout.setVisibility(View.GONE);
            pcLivePortraitViewManager.setScreenVisible(true, true);
            mPlayerContainer.setLayoutParams(getVideoSizeParams());
            remoteRender.setLayoutParams(getVideoSizeParams());

            if (inDocFullMode) {
                // 判断之前是否在文档全屏模式则退出，并恢复竖屏展示状态
                if (floatingView != null) {
                    floatingView.removeView();
                    pc_live_main.addView(rlLiveTopLayout, 0);
                    rlLiveTopLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
                    floatingView.quit();
                    floatingView = null;
                }

                dwLive.docApplyNewConfig(newConfig);
                inDocFullMode = false;
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (toDocFullMode) {
                // 采取尝试弹窗的方式 -- 因为：ContextCompat.checkSelfPermission 直接检测存在机型问题
                try {
                    pc_live_main.removeView(rlLiveTopLayout);
                    floatingView = new LiveFloatingView(PcLivePlayActivity.this, rlLiveTopLayout);
                } catch (Exception e) {
                    // 将悬浮窗的弹出时的崩溃捕获，并展示之前的文档全屏效果，不做视频的全屏展示
                    pc_live_main.addView(rlLiveTopLayout, 0);
                    rlLiveTopLayout.setVisibility(View.GONE);
                }
                rlLandscapeLayout.setVisibility(View.GONE);
                rlPortraitLayout.setVisibility(View.GONE);
                rlLiveInfosLayout.setVisibility(View.VISIBLE);
                tagRadioGroup.setVisibility(View.GONE);
                dwLive.docApplyNewConfig(newConfig);
                toDocFullMode = false;
                inDocFullMode = true;
            } else {
                setPortraitLayoutVisibility(View.GONE);
                rlLandscapeLayout.setVisibility(View.VISIBLE);
                pcLiveLandscapeViewManager.setScreenVisible(true, true);
                mPlayerContainer.setLayoutParams(getVideoSizeParams());
                remoteRender.setLayoutParams(getVideoSizeParams());
            }
        }

        blPcBarrage.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        blPcBarrage.init();
    }



    private View mRoot;
    private DWLivePlayer player;
    private DWLive dwLive = DWLive.getInstance();

    private WindowManager wm;

    private boolean hasLoadedHistoryChat; // 是否加载过了历史聊天

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pc_live;
    }


    RtcPopup rtcPopup;

    @Override
    protected void onViewCreated() {
        hasLoadedHistoryChat = false;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mRoot = getWindow().getDecorView().findViewById(android.R.id.content);
        // 屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }

        onSoftInputChange();

        initClosePopup();

        initLotteryPopup();

        initRollcallPopup();

        initVotePopup();

        initQuestionnairePopup();

        // 1 先定义View
        initViewPager();

        // 2 创建播放器然后设置相关参数
        initPlayer();

        // 3 初始化界面管理
        initPcLiveViewManager();

        blPcBarrage.start();
    }

    //-------------------------- CC SDK 直播视频 生命周期相关 Start -------------------------------

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
            dwLive.start(surface);
            isOnResumeStart = true;
        }
    }

    boolean isOnPause = false;

    @Override
    protected void onPause() {

        isPrepared = false;
        isOnPause = true;

        // 如果当前存在悬浮窗，就退出悬浮窗
        if (floatingView != null) {
            floatingView.removeView();
            pc_live_main.addView(rlLiveTopLayout, 0);
            rlLiveTopLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
            floatingView.quit();
            floatingView = null;
        }

        if (player != null && player.isPlaying()) {
            player.pause();
        }

        hideSpeak();

        if (qaLayoutController != null) {
            qaLayoutController.clearQaInfo();
        }


        pcLiveLandscapeViewManager.onPause();

        dwLive.stop();

        mRollcallPopup.dismissImmediate();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        pcLiveLandscapeViewManager.onDestroy();

        handler.removeCallbacks(dismissLottery);

        if (qaLayoutController != null) {
            qaLayoutController.removeTipsHideCallBack();
        }

        if (mRollcallPopup != null) {
            mRollcallPopup.onDestroy();
        }

        if (player != null) {
            player.pause();
            player.stop();
            player.release();
        }

        localRender.release();
        remoteRender.release();

        if (mSoftKeyBoardState != null) {
            mSoftKeyBoardState.release();
        }

        cancel10sTimerTask();
        stopCmTimer();

        dwLive.onDestroy();

        super.onDestroy();
    }

    Surface surface;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surface = new Surface(surfaceTexture);
        if (player.isPlaying()) {
            player.setSurface(surface);
        } else {
            dwLive.start(surface);
        }
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

    //-------------------------- CC SDK 直播视频 生命周期相关 END -----------------------------

    //-------------------------- CC SDK 直播视频 播放器相关 Start -----------------------------

    private void initPlayer() {
        mPlayerContainer.setSurfaceTextureListener(this);
        player = new DWLivePlayer(this);
        player.setOnPreparedListener(this);
        player.setOnVideoSizeChangedListener(this);
        dwLive.setDWLivePlayParams(myDWLiveListener, this, docView, player);
        initRtc();
    }

    boolean isPrepared = false;

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        isPrepared = true;
        player.start();

        pcPortraitProgressBar.setVisibility(View.GONE);
        tvPcPortraitStatusTips.setVisibility(View.GONE);

        if (rtcPopup.isShow()) {
            return;
        }

        if (isPortrait()) {
            setPortraitLayoutVisibility(View.VISIBLE);
        } else {
            rlLandscapeLayout.setVisibility(View.VISIBLE);
            pcLiveLandscapeViewManager.setScreenVisible(true, false);
        }
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
        int height= 0;
        if(isPortrait()) {
            height = wm.getDefaultDisplay().getHeight() / 3;
        } else {
            height = wm.getDefaultDisplay().getHeight();
        }


        int vWidth = player.getVideoWidth();
        int vHeight = player.getVideoHeight();

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

    private void reloadVideo() {
        if (player.isPlaying() || !isPrepared) {
            // 播放到下一个关键帧的时候，声音就会恢复
            if (DWApplication.RTC_AUDIO) {
                player.setVolume(1.0f, 1.0f);
            }
            return;
        }

        try {
            dwLive.restartVideo(surface);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DWLiveException e) {
            e.printStackTrace();
        }
    }

    //-------------------------- CC SDK 直播视频 播放器相关 END -----------------------------

    // --------------------- 视频播放控制器界面相关逻辑  ---------------------

    PcLivePortraitViewManager pcLivePortraitViewManager;
    PcLiveLandscapeViewManager pcLiveLandscapeViewManager;

    private void initPcLiveViewManager() {
        pcLiveLandscapeViewManager = new PcLiveLandscapeViewManager(this, rlLandscapeLayout, mRoot, livingSign, rtcPopup, mImm);
        pcLiveLandscapeViewManager.init();
        pcLivePortraitViewManager = new PcLivePortraitViewManager(this, rlLiveTopLayout, mRoot, livingSign, rtcPopup, mImm);
        pcLivePortraitViewManager.init();
    }

    @OnClick(R.id.rl_pc_live_top_layout)
    void onPlayOnClick(View v) {
        if (isPortrait()) {
            pcLivePortraitViewManager.onPlayClick();
        } else {
            pcLiveLandscapeViewManager.OnPlayClick();
        }

        // TODO 测试历史文档切换
        // showDocChangeDialog();
    }

    private void showNorRtcIcon() {
        pcLivePortraitViewManager.showNormalRtcIcon();
        pcLiveLandscapeViewManager.showNormalRtcIcon();
    }

    private void setPortraitLayoutVisibility(int i) {
        rlPortraitLayout.setVisibility(i);
        rlLiveInfosLayout.setVisibility(i);
        pcLivePortraitViewManager.setScreenVisible(true, false);
    }

    public void setRlSoundLayout(int i) {
        rlSoundLayout.setVisibility(i);
    }

    //-------------------------- CC SDK 直播视频 连麦相关 Start -----------------------------

    /** 初始化连麦模块 */
    private void initRtc() {
        rtcPopup = new RtcPopup(this);
        EglBase rootEglBase = EglBase.create();
        localRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);

        localRender.setMirror(true);
        localRender.setZOrderMediaOverlay(true); // 设置让本地摄像头置于最顶层

        dwLive.setRtcClientParameters(rtcClientListener, localRender, remoteRender);
    }

    public boolean isRtc = false;

    public void onApplyRtc() {

        if (!isNetworkConnected()) {
            Toast.makeText(getApplicationContext(), "没有网络，请检查", Toast.LENGTH_SHORT).show();
            return;
        }

        isRtc = true;
        livingSign.setVisibility(View.INVISIBLE);
        // 可选择音频连麦or视频连麦
        if (DWApplication.RTC_AUDIO) {
            dwLive.startVoiceRTCConnect();
        } else {
            dwLive.startRtcConnect();
        }
    }

    public void onCancelRtc() {
        dwLive.disConnectApplySpeak();
        hideSpeak();
    }

    public void onHangupRtc() {
        dwLive.disConnectSpeak();
        hideSpeak();

    }


    // 远程视频的宽高
    private int[] mVideoSizes = new int[2];

    public boolean isSpeaking = false;

    private void hideVideoRenderAndTips() {
        if (localRender != null) {
            localRender.setVisibility(View.GONE);
        }

        if (remoteRender != null) {
            remoteRender.setVisibility(View.GONE);
        }
    }

    public boolean isAllowRtc = false;
    private RtcClient.RtcClientListener rtcClientListener = new RtcClient.RtcClientListener() {
        @Override
        public void onAllowSpeakStatus(final boolean isAllowSpeak) {

            isAllowRtc = isAllowSpeak;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAllowSpeak && isSpeaking) {
                        return;
                    }

                    hideVideoRenderAndTips();
                    rtcPopup.resetView();
                    if (!isAllowSpeak) {
                        isSpeaking = false;
                        isRtc = false;
                        if (mPlayerContainer != null) {
                            mPlayerContainer.setVisibility(View.VISIBLE);
                        }
                        reloadVideo();
                        rtcPopup.dismiss();

                        pcLivePortraitViewManager.showNormalRtcIcon();
                        pcLiveLandscapeViewManager.showNormalRtcIcon();

                    }
                }
            });
        }

        AppRTCAudioManager mAudioManager;

        private void processRemoteVideoSize(String videoSize) {
            String[] sizes = videoSize.split("x");
            int width = Integer.parseInt(sizes[0]);
            int height = Integer.parseInt(sizes[1]);
            double ratio = (double) width / (double) height;
            // 对于分辨率为16：9的，更改默认分辨率为16：10
            if (ratio > 1.76 && ratio < 1.79) {
                mVideoSizes[0] = 1600;
                mVideoSizes[1] = 1000;
            }
        }

        @Override
        public void onEnterSpeak(final String videoSize) {

            processRemoteVideoSize(videoSize);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isSpeaking) {
                        return;
                    }

                    // 根据连麦模式执行不同的UI处理
                    if (DWApplication.RTC_AUDIO) {
                        player.setVolume(0f, 0f);
                    } else {
                        player.pause();
                        player.stop();
                        if (mPlayerContainer != null) {
                            mPlayerContainer.setVisibility(View.INVISIBLE);
                        }
                        localRender.setVisibility(View.INVISIBLE);
                        remoteRender.setVisibility(View.VISIBLE);
                        remoteRender.setLayoutParams(getRemoteRenderSizeParams());
                    }

                    // 由于rtc是走的通话音频，所以需要做处理
                    mAudioManager = AppRTCAudioManager.create(PcLivePlayActivity.this, null);
                    mAudioManager.init();
                    isSpeaking = true;

                    //设置为视频模式
                    if (!isVideo) {
                        isVideo = true;
                        dwLive.setDefaultPlayMode(DWLive.PlayMode.VIDEO);
                        setRlSoundLayout(View.INVISIBLE);
                        changeVideoAudioIcon();
                    }

                    dwLive.removeLocalRender();

                    rtcPopup.showConnectedView();

                    startCmTimer();
                }
            });

        }

        @Override
        public void onDisconnectSpeak() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mAudioManager != null) {
                        mAudioManager.close();
                    }

                    hideSpeak();
                }
            });
        }

        @Override
        public void onSpeakError(final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    if (mAudioManager != null) {
                        mAudioManager.close();
                    }
                    hideSpeak();
                }
            });
        }

        @Override
        public void onCameraOpen(final int width, final int height) {
        }
    };


    private Timer cmTimer;
    private TimerTask cmTimerTask;

    // 增加一个间隔为1s的定时器，如果断网，则增加一个10s的延时器，超过10s，重置dwlive
    private void startCmTimer() {
        cmCount = 0;

        if (cmTimer == null) {
            cmTimer = new Timer();
        }

        if (cmTimerTask != null) {
            cmTimerTask.cancel();
        }

        cmTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rtcPopup.setCounterText(cmCount++);
                        if (!isNetworkConnected()) {
                            start10sTimerTask();
                        } else {
                            cancel10sTimerTask();
                        }
                    }
                });
            }
        };

        cmTimer.schedule(cmTimerTask, 0, 1000);
    }


    private int cmCount;

    private void stopCmTimer() {

        if (cmTimerTask != null) {
            cmTimerTask.cancel();
        }
    }

    private TimerTask cm10sTimerTask;

    private void start10sTimerTask() {
        if (cm10sTimerTask != null) {
            return;
        }

        cm10sTimerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dwLive.disConnectSpeak();
                        hideSpeak();
                        stopCmTimer();
                        cancel10sTimerTask();
                        rtcPopup.showNoNetworkView();
                    }
                });
            }
        };

        cmTimer.schedule(cm10sTimerTask, 10 * 1000);
    }

    private void cancel10sTimerTask() {
        if (cm10sTimerTask != null) {
            cm10sTimerTask.cancel();
            cm10sTimerTask = null;
        }

    }

    private void hideSpeak() {
        if (isRtc || isSpeaking) {
            dwLive.closeCamera();
            hideVideoRenderAndTips();
            if (mPlayerContainer != null) {
                mPlayerContainer.setVisibility(View.VISIBLE);
            }
            isRtc = false;
            isSpeaking = false;
            stopCmTimer();

            rtcPopup.resetView();
            showNorRtcIcon();

            reloadVideo();
        }
    }


    // 连麦远端视频组件等比缩放
    private RelativeLayout.LayoutParams getRemoteRenderSizeParams() {
        int width = 600;
        int height = 400;

        if (isPortrait()) {
            width = wm.getDefaultDisplay().getWidth();
            height = wm.getDefaultDisplay().getHeight() / 3; //TODO 根据当前布局更改
        } else {
            width = wm.getDefaultDisplay().getWidth();
            height = wm.getDefaultDisplay().getHeight();
        }

        int vWidth = mVideoSizes[0];
        int vHeight = mVideoSizes[1];

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


    //-------------------------- CC SDK 直播视频 连麦相关 END -----------------------------

    Handler handler = new Handler(Looper.getMainLooper());

    // 可能出现没有username的情况，故先存储下来
    private Map<String, String> userInfoMap = new HashMap<String, String>();


    /** 直播回调接口（核心）*/
    private DWLiveListener myDWLiveListener = new DWLiveListener() {
        @Override
        public void onQuestion(final Question question) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (qaLayoutController != null) {
                        qaLayoutController.addQuestion(question);
                    }
                }
            });
        }

        @Override
        public void onPublishQuestion(final String questionId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (qaLayoutController != null) {
                        qaLayoutController.showQuestion(questionId);
                    }
                }
            });
        }

        @Override
        public void onAnswer(final Answer answer) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (qaLayoutController != null) {
                        qaLayoutController.addAnswer(answer);
                    }
                }
            });
        }

        @Override
        public void onLiveStatus(final DWLive.PlayStatus playStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (playStatus) {
                        case PLAYING:
                            hideVideoRenderAndTips();
                            pcPortraitProgressBar.setVisibility(View.VISIBLE);
                            tvPcPortraitStatusTips.setVisibility(View.GONE);
                            if (docView != null) {
                                docView.setVisibility(View.VISIBLE);
                            }

                            break;
                        case PREPARING:
                            pcPortraitProgressBar.setVisibility(View.GONE);
                            tvPcPortraitStatusTips.setVisibility(View.VISIBLE);
                            tvPcPortraitStatusTips.setText("直播尚未开始！");
                            break;
                    }

                }
            });

        }

        @Override
        public void onStreamEnd(boolean b) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (docView != null) {
                        docView.clearDrawInfo();
                        docView.setVisibility(View.GONE);
                    }

                    isAllowRtc = false;

                    player.pause();
                    player.stop();
                    player.reset();

                    pcPortraitProgressBar.setVisibility(View.GONE);
                    tvPcPortraitStatusTips.setVisibility(View.VISIBLE);
                    tvPcPortraitStatusTips.setText("直播已结束！");
                }
            });

        }

        @Override
        public void onHistoryChatMessage(final ArrayList<ChatMessage> chatLogs) {

            // 如果之前已经加载过了历史聊天信息，就不再接收
            if (hasLoadedHistoryChat) {
                return;
            }

            // 历史聊天信息
            if (chatLogs == null || chatLogs.size() == 0) {
                Log.e("onHistoryChatMessage", "无历史聊天信息");
                return;
            }

            hasLoadedHistoryChat = true;
            // 注：历史聊天信息中 ChatMessage 的 currentTime = ""
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 将历史聊天信息添加到UI
                    for (int i = 0; i < chatLogs.size(); i++) {
                        if (blPcBarrage != null) {
                            blPcBarrage.addNewInfo(chatLogs.get(i).getMessage());
                        }
                        if (chatLayoutController != null) {
                            chatLayoutController.addChatEntity(getChatEntity(chatLogs.get(i)));
                        }
                        userInfoMap.put(chatLogs.get(i).getUserId(), chatLogs.get(i).getUserName());
                    }
                }
            });
        }

        @Override
        public void onPublicChatMessage(final ChatMessage chatMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (blPcBarrage != null) {
                        blPcBarrage.addNewInfo(chatMessage.getMessage());
                    }

                    if (chatLayoutController != null) {
                        chatLayoutController.addChatEntity(getChatEntity(chatMessage));
                    }

                    userInfoMap.put(chatMessage.getUserId(), chatMessage.getUserName());

                }
            });
        }

        @Override
        public void onSilenceUserChatMessage(final ChatMessage chatMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (blPcBarrage != null) {
                        blPcBarrage.addNewInfo(chatMessage.getMessage());
                    }

                    if (chatLayoutController != null) {
                        chatLayoutController.addChatEntity(getChatEntity(chatMessage));
                    }

                }
            });
        }

        @Override
        public void onPrivateQuestionChatMessage(ChatMessage chatMessage) {

        }

        @Override
        public void onPrivateAnswerChatMessage(ChatMessage chatMessage) {

        }

        @Override
        public void onPrivateChat(final PrivateChatInfo info) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatLayoutController.updatePrivateChat(getChatEntity(info, false));
                }
            });
        }

        @Override
        public void onPrivateChatSelf(final PrivateChatInfo info) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chatLayoutController.updatePrivateChat(getChatEntity(info, true));
                }
            });
        }

        @Override
        public void onUserCountMessage(final int i) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pcLiveLandscapeViewManager.onUserCountMsg(i);
                    pcLivePortraitViewManager.onUserCountMsg(i);
                }
            });
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
        public void onNotification(String s) {
            Log.e("onNotification", s);
        }

        /**
         * 收到历史广播信息(目前服务端只返回最后一条历史广播)
         *
         * @param msgs 广播消息列表
         */
        @Override
        public void onHistoryBroadcastMsg(final ArrayList<BroadCastMsg> msgs) {
            // 判断空
            if (msgs == null) {
                return;
            }
            // 展示历史广播信息
            for (int i = 0; i < msgs.size(); i++) {
                showBroadcastMsg(msgs.get(i).getContent());
            }
        }

        /**
         * 收到广播信息（实时）
         *
         * @param msg 广播消息
         */
        @Override
        public void onBroadcastMsg(String msg) {
            // 展示广播信息
            showBroadcastMsg(msg);
        }

        @Override
        public void onInformation(final String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PcLivePlayActivity.this, s, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onException(DWLiveException e) {

        }

        @Override
        public void onInitFinished(final int i, final List<QualityInfo> list) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pcLiveLandscapeViewManager.onInitFinish(i, list, surface);
                    pcLivePortraitViewManager.onInitFinish(i, list, surface);
                }
            });

        }

        @Override
        public void onKickOut() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "您已被踢出直播间", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        @Override
        public void onLivePlayedTime(int i) {
        }

        @Override
        public void onLivePlayedTimeException(Exception e) {
        }

        @Override
        public void isPlayedBack(boolean b) {
        }

        /**
         * 统计需要使用的参数
         * @deprecated 已弃用
         * @param map 统计相关参数
         */
        @Override
        public void onStatisticsParams(Map<String, String> map) {
            // 不需要在此回调里实现任何逻辑
        }

        @Override
        public void onCustomMessage(String s) {

        }

        /** 封禁 */
        @Override
        public void onBanStream(String reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 播放器停止播放
                    if (player != null) {
                        player.stop();
                    }
                    // 展示'直播间已封禁'的标识
                    if (pcPortraitProgressBar != null) {
                        pcPortraitProgressBar.setVisibility(View.GONE);
                    }
                    if (tvPcPortraitStatusTips != null) {
                        tvPcPortraitStatusTips.setVisibility(View.VISIBLE);
                        tvPcPortraitStatusTips.setText("直播间已封禁");
                    }
                }
            });
        }

        /** 解封禁 */
        @Override
        public void onUnbanStream() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (surface != null) {
                        dwLive.start(surface);
                    }
                    hideVideoRenderAndTips();
                    if (pcPortraitProgressBar != null) {
                        pcPortraitProgressBar.setVisibility(View.VISIBLE);
                    }
                    if (tvPcPortraitStatusTips != null) {
                        tvPcPortraitStatusTips.setVisibility(View.GONE);
                    }
                }
            });
        }

        /**
         * 公告
         *
         * @param isRemove     是否是公告删除，如果为true，表示公告删除且announcement参数为null
         * @param announcement 公告内容
         */
        @Override
        public void onAnnouncement(final boolean isRemove, final String announcement) {
            showNewAnnounce(isRemove, announcement);
        }


        /**
         * 签到回调
         *
         * @param duration 签到持续时间，单位为秒
         */
        @Override
        public void onRollCall(int duration) {
            startRollCall(duration);
        }

        /**
         * 开始抽奖
         *
         * @param lotteryId 本次抽奖的id
         */
        @Override
        public void onStartLottery(String lotteryId) {
            startLottery(lotteryId);
        }


        /**
         * 抽奖结果
         *
         * @param isWin       是否中奖，true表示中奖了
         * @param lotteryCode 中奖码
         * @param lotteryId   本次抽奖的id
         * @param winnerName  中奖者的名字
         */
        @Override
        public void onLotteryResult(final boolean isWin, final String lotteryCode, final String lotteryId, final String winnerName) {
            showLotteryResult(isWin, lotteryCode, lotteryId, winnerName);
        }


        /**
         * 结束抽奖
         *
         * @param lotteryId 本次抽奖的id
         */
        @Override
        public void onStopLottery(String lotteryId) {
            stopLottery(lotteryId);
        }

        /**
         * 开始投票
         *
         * @param voteCount 总共的选项个数2-5
         * @param VoteType  0表示单选，1表示多选，目前只有单选
         */
        @Override
        public void onVoteStart(final int voteCount, final int VoteType) {
            // 开始投票
            startVote(voteCount, VoteType);
        }

        /**
         * 结束投票
         */
        @Override
        public void onVoteStop() {
            // 结束投票
            stopVote();
        }

        /**
         * 投票结果统计
         */
        @Override
        public void onVoteResult(final JSONObject jsonObject) {
            // 展示投票结果统计
            showVoteResult(jsonObject);
        }

        /**
         * 发布问卷
         *
         * @param info 问卷内容
         */
        @Override
        public void onQuestionnairePublish(final QuestionnaireInfo info) {
            // 开始问卷答题
            startQuestionnaire(info);
        }

        /**
         * 停止问卷
         */
        public void onQuestionnaireStop(final String questionnaireId) {
            // 停止问卷答题
            stopQuestionnaire();
        }

        /**
         * 问卷统计信息
         */
        @Override
        public void onQuestionnaireStatis(QuestionnaireStatisInfo info) {
            // 展示问卷统计信息
            showQuestionnaireStatis(info);
        }

        /**
         * 发布第三方问卷
         *
         * @param title 问卷标题
         * @param externalUrl 第三方问卷链接
         */
        public void onExeternalQuestionnairePublish(String title, String externalUrl) {
            // 展示第三方问卷
            showExeternalQuestionnaire(title, externalUrl);
        }
    };



    // --------------------- 聊天功能辅助方法  ---------------------

    private ChatEntity getChatEntity(ChatMessage msg) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(msg.getUserId());
        chatEntity.setUserName(msg.getUserName());
        chatEntity.setPrivate(!msg.isPublic());

        if (msg.getUserId().equals(dwLive.getViewer().getId())) {
            chatEntity.setPublisher(true);
        } else {
            chatEntity.setPublisher(false);
        }

        chatEntity.setMsg(msg.getMessage());
        chatEntity.setTime(msg.getTime());
        chatEntity.setUserAvatar(msg.getAvatar());
        return chatEntity;
    }

    private ChatEntity getChatEntity(PrivateChatInfo info, boolean isPublisher) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(info.getFromUserId());
        chatEntity.setUserName(info.getFromUserName());
        chatEntity.setPrivate(true);
        chatEntity.setReceiveUserId(info.getToUserId());

        if (info.getToUserName() == null && userInfoMap.containsKey(info.getToUserId())) {
            info.setToUserName(userInfoMap.get(info.getToUserId()));
        }
        chatEntity.setReceivedUserName(info.getToUserName());
        chatEntity.setReceiveUserAvatar("");
        chatEntity.setPublisher(isPublisher);
        chatEntity.setMsg(info.getMsg());
        chatEntity.setTime(info.getTime());
        chatEntity.setUserAvatar("");
        return chatEntity;
    }

    // --------------------- 音视频模式切换  ---------------------

    private boolean isVideo = true;

    //监听音视频变化
    public void changeVideoAudioStatus() {
        if (isVideo) {
            isVideo = false;
            dwLive.changePlayMode(DWLive.PlayMode.SOUND);
            setRlSoundLayout(View.VISIBLE);
        } else {
            isVideo = true;
            dwLive.changePlayMode(DWLive.PlayMode.VIDEO);
            setRlSoundLayout(View.INVISIBLE);
        }

        changeVideoAudioIcon();
    }

    private void changeVideoAudioIcon() {
        pcLivePortraitViewManager.onVideoAudioChanged(isVideo);
        pcLiveLandscapeViewManager.onVideoAudioChanged(isVideo);
    }


    // --------------------- 线路切换  ---------------------

    public void changeSource(boolean isPortartLayout, int selectItem) {

        if (isPortartLayout) {
            pcLiveLandscapeViewManager.updateSourceSelectItem(selectItem);
        } else {
            pcLivePortraitViewManager.updateSourceSelectItem(selectItem);
        }

        dwLive.changePlaySource(selectItem);
    }

    // --------------------- 弹幕功能  ---------------------

    private boolean isBarrageOn = true;

    /** 更改弹幕功能状态 */
    public void changeBarrageStatus() {
        if (isBarrageOn) {
            blPcBarrage.setVisibility(View.GONE);
            blPcBarrage.stop();
            isBarrageOn = false;
        } else {
            blPcBarrage.setVisibility(View.VISIBLE);
            blPcBarrage.start();
            isBarrageOn = true;
        }

        changeBarrageIcon();
    }

    /** 更改弹幕功能图标 */
    private void changeBarrageIcon() {
        pcLivePortraitViewManager.onBarrageChanged(isBarrageOn);
        pcLiveLandscapeViewManager.onBarrageChanged(isBarrageOn);
    }

    // ---------------------- Demo 功能实现 广播、公告、签到、抽奖、投票、问卷 Start -----------------



    // --------------------- 广播  ---------------------
    /** 展示广播内容 */
    private void showBroadcastMsg(final String msg) {

        // 判断空
        if (msg == null || msg.isEmpty()) {
            return;
        }

        // 目前demo实现的是将广播放到聊天区域展示
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (chatLayoutController != null) {
                    // 构建一个对象
                    ChatEntity chatEntity = new ChatEntity();
                    chatEntity.setUserId("");
                    chatEntity.setUserName("");
                    chatEntity.setPrivate(false);
                    chatEntity.setPublisher(true);
                    chatEntity.setMsg("系统消息: " + msg);
                    chatEntity.setTime("");
                    chatEntity.setUserAvatar("");
                    chatLayoutController.addChatEntity(chatEntity);
                }
            }
        });
    }

    // --------------------- 公告  ---------------------

    /** 展示新公告 */
    public void showNewAnnounce(final boolean isRemove, final String announcement) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pcLivePortraitViewManager.isAnnouncementShown() || pcLiveLandscapeViewManager.isAnnouncementShown()) {
                    pcLiveLandscapeViewManager.onNewAnnounce(isRemove, announcement, true);
                    pcLivePortraitViewManager.onNewAnnounce(isRemove, announcement, true);
                } else {
                    pcLiveLandscapeViewManager.onNewAnnounce(isRemove, announcement, false);
                    pcLivePortraitViewManager.onNewAnnounce(isRemove, announcement, false);
                }
            }
        });
    }

    /** 展示直播间公告 */
    public void onShowAnnounce() {
        pcLiveLandscapeViewManager.onShowAnnouce();
        pcLivePortraitViewManager.onShowAnnounce();
    }

    // --------------------- 签到  ---------------------

    private RollCallPopup mRollcallPopup;

    /** 初始化签到的弹出界面 */
    private void initRollcallPopup() {
        mRollcallPopup = new RollCallPopup(this);
    }

    /** 开始签到 */
    public void startRollCall(final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRollcallPopup != null) {
                    mRollcallPopup.show(mRoot);
                    mRollcallPopup.startRollcall(duration);
                }
            }
        });
    }

    // --------------------- 抽奖  ---------------------

    private LotteryStartPopup mLotteryStartPopup; // 开始抽奖弹出框
    private LotteryPopup mLotteryPopup;  // 抽奖结果弹出框

    boolean isLotteryWin = false;

    int lotteryDelay = 3 * 1000;

    Runnable dismissLottery = new Runnable() {
        @Override
        public void run() {
            if (mLotteryPopup != null && mLotteryPopup.isShowing()) {
                mLotteryPopup.dismiss();
            }
        }
    };

    /** 初始化抽奖的弹出界面 */
    private void initLotteryPopup() {
        mLotteryStartPopup = new LotteryStartPopup(this);
        mLotteryStartPopup.setKeyBackCancel(true);
        mLotteryPopup = new LotteryPopup(this);
        mLotteryPopup.setKeyBackCancel(true);
    }

    /** 开始抽奖 */
    public void startLottery(String lotteryId) {
        isLotteryWin = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLotteryStartPopup.show(mRoot);
                mLotteryStartPopup.startLottery();
            }
        });
    }

    /** 展示抽奖结果 */
    public void showLotteryResult(final boolean isWin, final String lotteryCode, final String lotteryId, final String winnerName) {
        // 如果已经中奖了，而且中奖界面没有关闭，则不做后续的界面处理
        if (isLotteryWin && mLotteryPopup.isShowing()) {
            return;
        }

        this.isLotteryWin = isWin;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLotteryPopup.show(mRoot);
                mLotteryPopup.onLotteryResult(isWin, lotteryCode, winnerName);

                if (!isLotteryWin) {
                    handler.postDelayed(dismissLottery, lotteryDelay);
                }
            }
        });
    }

    /** 停止抽奖 */
    public void stopLottery(String lotteryId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLotteryStartPopup != null && mLotteryStartPopup.isShowing()) {
                    mLotteryStartPopup.dismiss();
                }

                if (!isLotteryWin) {
                    handler.postDelayed(dismissLottery, lotteryDelay);
                }
            }
        });
    }

    // --------------------- 投票  ---------------------

    private VotePopup mVotePopup;

    boolean isVoteResultShow = false;

    /** 初始化投票的弹出界面 */
    private void initVotePopup() {
        mVotePopup = new VotePopup(this);
    }

    /** 开始投票 */
    private void startVote(final int voteCount, final int VoteType) {
        isVoteResultShow = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVotePopup.startVote(voteCount, VoteType);
                mVotePopup.show(mRoot);
            }
        });
    }

    /** 结束投票 */
    private void stopVote() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVoteResultShow) {
                    return;
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mVotePopup.dismiss();
                        }
                    });
                }
            }
        }, 1000);
    }

    /** 展示投票结果统计 */
    private void showVoteResult(final JSONObject jsonObject) {
        isVoteResultShow = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVotePopup.onVoteResult(jsonObject);
                mVotePopup.show(mRoot);
            }
        });
    }

    // --------------------- 问卷  ---------------------

    private QuestionnairePopup mQuestionnairePopup;  // 问卷弹出界面
    private QuestionnaireStopPopup mQuestionnaireStopPopup; // 问卷结束弹出界面
    private ExeternalQuestionnairePopup mExeternalQuestionnairePopup; // 第三方问卷弹出界面
    private QuestionnaireStatisPopup mQuestionnaireStatisPopup; // 问卷统计界面

    /** 初始化问卷的弹出界面 */
    private void initQuestionnairePopup() {
        mQuestionnairePopup = new QuestionnairePopup(this);
        mExeternalQuestionnairePopup = new ExeternalQuestionnairePopup(this);

        mQuestionnaireStopPopup = new QuestionnaireStopPopup(this);
        mQuestionnaireStopPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (mQuestionnairePopup != null) {
                    mQuestionnairePopup.dismiss();
                }
            }
        });

        mQuestionnaireStatisPopup = new QuestionnaireStatisPopup(this);
    }

    /** 开始问卷答题 */
    private void startQuestionnaire(final QuestionnaireInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mQuestionnairePopup.setQuestionnaireInfo(info);
                mQuestionnairePopup.show(mRoot);
            }
        });
    }

    /** 停止问卷答题 */
    private void stopQuestionnaire() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mQuestionnairePopup != null && mQuestionnairePopup.isShowing()) {
                    if (!mQuestionnairePopup.hasSubmitedQuestionnaire()) {
                        mQuestionnaireStopPopup.show(mRoot);
                    }
                }
            }
        });
    }

    /** 展示问卷统计信息 */
    private void showQuestionnaireStatis(final QuestionnaireStatisInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mQuestionnaireStatisPopup.setQuestionnaireStatisInfo(info);
                mQuestionnaireStatisPopup.show(mRoot);
            }
        });
    }

    /** 展示第三方问卷 */
    private void showExeternalQuestionnaire(final String title, final String externalUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mExeternalQuestionnairePopup != null) {
                    mExeternalQuestionnairePopup.setQuestionnaireInfo(title, externalUrl);
                    mExeternalQuestionnairePopup.show(mRoot);
                }
            }
        });
    }


    // ---------------------- Demo 功能实现 广播、公告、签到、抽奖、投票、问卷 END -----------------


    // --------------------- Demo 视频下方布局 问答、聊天、问答、简介布局 Start --------------------

    @BindView(R.id.rg_infos_tag)
    RadioGroup tagRadioGroup;

    @BindView(R.id.live_portrait_info_document)
    RadioButton docTag;

    @BindView(R.id.live_portrait_info_chat)
    RadioButton chatTag;

    @BindView(R.id.live_portrait_info_qa)
    RadioButton qaTag;

    @BindView(R.id.live_portrait_info_intro)
    RadioButton introTag;

    @BindView(R.id.live_portrait_container_viewpager)
    ViewPager infoLayoutContainer;

    List<View> infoList = new ArrayList<>();
    List<Integer> tagIdList = new ArrayList<>();
    List<RadioButton> tagRBList = new ArrayList<>();

    View docLayout;
    View chatLayout;
    View qaLayout;
    View introLayout;

    DocLayoutController docLayoutController;
    ChatLayoutController chatLayoutController;
    QaLayoutController qaLayoutController;
    IntroLayoutController introLayoutController;

    private final static String VIEW_VISIBLE_TAG = "1";

    // 双击全屏相关
    boolean isMove = false;
    private final static int DOUBLE_TAP_TIMEOUT = 200;
    private MotionEvent mPreviousUpEvent;

    private boolean toDocFullMode;  // 是否要进入文档全屏模式
    private boolean inDocFullMode;  // 当前是否在文档全屏模式

    private DocView docView;

    // 初始化下方布局的ViewPager
    private void initViewPager() {

        LayoutInflater inflater = LayoutInflater.from(this);

        initLayout(inflater);

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
                hideKeyboard();
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


        if (tagRBList.contains(docTag)) {
            docTag.performClick();
        } else {
            tagRBList.get(0).performClick();
        }
    }

    // 初始化下方布局区域
    private void initLayout(LayoutInflater inflater) {

        // 获取当前直播间的模版信息
        TemplateInfo templateInfo = dwLive.getTemplateInfo();

        // 判断空
        if (templateInfo == null) {
            return;
        }

        // 判断当前直播间是否有文档
        if (VIEW_VISIBLE_TAG.equals(dwLive.getTemplateInfo().getPdfView())) {

            // 初始化文档布局
            initDocLayout(inflater);
            docView = docLayoutController.getDocView();
            docView.setClickable(true);

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
                                onShowDocFull();
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

        // 判断当前直播间是否有聊天
        if (VIEW_VISIBLE_TAG.equals(dwLive.getTemplateInfo().getChatView())) {
            // 初始化聊天布局
            initChatLayout(inflater);
        }

        // 判断当前直播间是否有问答
        if (VIEW_VISIBLE_TAG.equals(dwLive.getTemplateInfo().getQaView())) {
            // 初始化问答布局
            initQaLayout(inflater);
        }

        // 初始化简介布局
        initIntroLayout(inflater);
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

    // 初始化简介布局区域
    private void initIntroLayout(LayoutInflater inflater) {
        tagIdList.add(R.id.live_portrait_info_intro);
        tagRBList.add(introTag);
        introTag.setVisibility(View.VISIBLE);
        introLayout = inflater.inflate(R.layout.live_portrait_intro_layout, null);
        infoList.add(introLayout);

        introLayoutController = new IntroLayoutController(this, introLayout);
        introLayoutController.initIntro();
    }

    // 切换至文档全屏 -- 具体文档全屏逻辑在 onConfigurationChanged 方法中
    public void onShowDocFull() {
        if (isPortrait()) {
            toDocFullMode = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }


    //--------------------- Demo 视频下方布局 问答、聊天、问答、简介布局 END --------------------


    //-------------------------- Demo 退出观看页逻辑 --------------------------

    private CommonPopup mExitPopup; // 退出界面弹出框

    /** 初始化结束观看弹出界面 */
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

    public void showClosePopupWindow() {
        mExitPopup.show(mRoot);
    }

    // Back 键相关逻辑处理
    @Override
    public void onBackPressed() {
        if (!isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        } else {
            if (pcLiveLandscapeViewManager.onBackPressed()) {
                return;
            }

            if (chatLayoutController != null && chatLayoutController.onBackPressed()) {
                return;
            }
        }

        mExitPopup.show(mRoot);
    }

    //-------------------------- Demo 观看页 工具方法 -------------------------------

    private InputMethodManager mImm;

    // 软键盘监听
    private SoftKeyBoardState mSoftKeyBoardState;

    private void onSoftInputChange() {
        mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mSoftKeyBoardState = new SoftKeyBoardState(mRoot, false);
        mSoftKeyBoardState.setOnSoftKeyBoardStateChangeListener(new SoftKeyBoardState.OnSoftKeyBoardStateChangeListener() {
            @Override
            public void onChange(boolean isShow) {
                pcLiveLandscapeViewManager.onSoftKeyChange(isShow);
            }
        });
    }

    // 隐藏输入法
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(rlLiveTopLayout.getWindowToken(), 0);
        }
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

    // 判断当前屏幕朝向是否为竖屏
    private boolean isPortrait() {
        int mOrientation = getApplicationContext().getResources().getConfiguration().orientation;
        return mOrientation != Configuration.ORIENTATION_LANDSCAPE;
    }

    // 检测网络是否可用
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isAvailable();
    }

    /**
     * 展示文档切换的对话框
     */
    private void showDocChangeDialog() {
        HashMap<String, RoomDocInfo> docInfos = dwLive.getRoomDocInfos();

        final ArrayList<Integer> it = new ArrayList<>();
        final ArrayList<String> st = new ArrayList<>();

        int length = 0;

        for(Map.Entry<String, RoomDocInfo> entry: docInfos.entrySet()) {
            length += entry.getValue().getPages().size();
        }

        for(RoomDocInfo docInfo : docInfos.values()) {
            length += docInfo.getPages().size();
        }

        final String items[] = new String[length];

        int index = 0;
        for(Map.Entry<String, RoomDocInfo> entry: docInfos.entrySet()) {
            for (int i = 0; i < entry.getValue().getPages().size(); i++) {
                it.add(entry.getValue().getPages().valueAt(i).getPageIndex());
                st.add(entry.getValue().getDocId());
                items[index] = entry.getValue().getDocName() + "-->" + entry.getValue().getPages().valueAt(i).getPageIndex();
                index++;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this,3);
        builder.setTitle("文档列表");
        // builder.setMessage("是否确认退出?"); //设置内容
        builder.setIcon(R.mipmap.ic_launcher);
        // 设置列表显示，注意设置了列表显示就不要设置builder.setMessage()了，否则列表不起作用。
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(PcLivePlayActivity.this, items[which], Toast.LENGTH_SHORT).show();
                dwLive.changePageTo(st.get(which), it.get(which));
            }
        });
        builder.setPositiveButton("切成自由模式", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dwLive.changeDocModeType(DWLive.DocModeType.FREE_MODE);
                Toast.makeText(PcLivePlayActivity.this, "切成自由模式", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("切成跟随模式", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dwLive.changeDocModeType(DWLive.DocModeType.NORMAL_MODE);
                Toast.makeText(PcLivePlayActivity.this, "切成跟随模式", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create().show();
    }

}