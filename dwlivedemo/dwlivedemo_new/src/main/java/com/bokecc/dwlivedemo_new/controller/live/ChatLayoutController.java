package com.bokecc.dwlivedemo_new.controller.live;


import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.adapter.EmojiAdapter;
import com.bokecc.dwlivedemo_new.adapter.LivePublicChatAdapter;
import com.bokecc.dwlivedemo_new.adapter.PrivateChatAdapter;
import com.bokecc.dwlivedemo_new.adapter.PrivateUserAdapter;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.dwlivedemo_new.module.ChatEntity;
import com.bokecc.dwlivedemo_new.module.PrivateUser;
import com.bokecc.dwlivedemo_new.recycle.BaseOnItemTouch;
import com.bokecc.dwlivedemo_new.recycle.OnClickListener;
import com.bokecc.dwlivedemo_new.util.EmojiUtil;
import com.bokecc.dwlivedemo_new.util.SoftKeyBoardState;
import com.bokecc.sdk.mobile.live.DWLive;
import com.bokecc.sdk.mobile.push.chat.model.ChatUser;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 直播聊天布局区域控制类
 */
public class ChatLayoutController extends BaseLayoutController {

    @BindView(R.id.chat_container)
    RecyclerView mChatList;

    @BindView(R.id.id_private_chat_user_layout)
    LinearLayout mPrivateChatUserLayout;

    @BindView(R.id.id_push_chat_layout)
    RelativeLayout mChatLayout;

    @BindView(R.id.id_push_chat_input)
    EditText mInput;

    @BindView(R.id.id_push_chat_emoji)
    ImageView mEmoji;

    @BindView(R.id.id_push_emoji_grid)
    GridView mEmojiGrid;

    @BindView(R.id.iv_live_pc_private_chat)
    ImageView mPrivateIcon;

    @BindView(R.id.id_private_chat_msg_layout)
    LinearLayout mPrivateChatMsgLayout;

    @BindView(R.id.id_private_chat_user_list)
    RecyclerView mPrivateChatUserList;

    @BindView(R.id.id_private_chat_msg_mask)
    FrameLayout mPrivateChatMsgMask;

    @BindView(R.id.id_private_chat_title)
    TextView mPrivateChatUserName;

    @BindView(R.id.id_private_chat_list)
    RecyclerView mPrivateChatMsgList;

    // 软键盘是否显示
    private boolean isSoftInput = false;
    // emoji是否需要显示 emoji是否显示
    private boolean isEmoji = false, isEmojiShow = false;
    // 聊天是否显示
    private boolean isChat = false;
    // 是否是私聊
    private boolean isPrivate = false;
    // 是否显示私聊用户列表
    private boolean isPrivateChatUser = false;
    // 是否显示私聊列表
    private boolean isPrivateChatMsg = false;
    private String mCurPrivateUserId = "";

    // 私聊用户列表适配器
    private PrivateUserAdapter mPrivateUserAdapter;
    // 私聊信息列表
    private PrivateChatAdapter mPrivateChatAdapter;
    private ChatUser mTo; // 私聊对象
    private ArrayList<ChatEntity> mPrivateChats; // 存放所有的私聊信息

    // 软键盘监听
    private SoftKeyBoardState mSoftKeyBoardState;

    private InputMethodManager mImm;

    private Context mContext;

    public ChatLayoutController(Context context, View view) {
        mContext = context;
        mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        ButterKnife.bind(this, view);
    }

    LivePublicChatAdapter mChatAdapter;

    public void initChat() {
        mChatList.setLayoutManager(new LinearLayoutManager(mContext));
        mChatAdapter = new LivePublicChatAdapter(mContext);
        mChatList.setAdapter(mChatAdapter);
        mChatList.addOnItemTouchListener(new BaseOnItemTouch(mChatList, new OnClickListener() {
            @Override
            public void onClick(RecyclerView.ViewHolder viewHolder) {
                int position = mChatList.getChildAdapterPosition(viewHolder.itemView);
                ChatEntity chatEntity = mChatAdapter.getChatEntities().get(position);
                click2PrivateChat(chatEntity, false);
            }
        }));

        mChatList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hideKeyboard();
                return false;
            }
        });

        initChatView();
    }


    private short maxInput = 300;
    public void initChatView() {

        mInput.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideEmoji();
                return false;
            }
        });

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                String inputText = mInput.getText().toString();

                if (inputText.length() > maxInput) {
                    toastOnUiThread(mContext, "字符数超过300字");
                    mInput.setText(inputText.substring(0, maxInput));
                    mInput.setSelection(maxInput);
                }
            }
        });

        EmojiAdapter emojiAdapter = new EmojiAdapter(mContext);
        emojiAdapter.bindData(EmojiUtil.imgs);
        mEmojiGrid.setAdapter(emojiAdapter);
        mEmojiGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mInput == null) {
                    return;
                }

                // 一个表情span占位8个字符
                if (mInput.getText().length() + 8 > maxInput) {
                    toastOnUiThread(mContext, "字符数超过300字");
                    return;
                }

                if (position == EmojiUtil.imgs.length - 1) {
                    EmojiUtil.deleteInputOne(mInput);
                } else {
                    EmojiUtil.addEmoji(mContext, mInput, position);
                }
            }
        });

        mPrivateChats = new ArrayList<>(); // 初始化私聊数据集合

        mPrivateChatUserList.setLayoutManager(new LinearLayoutManager(mContext));
        mPrivateUserAdapter = new PrivateUserAdapter(mContext);
        mPrivateChatUserList.setAdapter(mPrivateUserAdapter);
        mPrivateChatUserList.addOnItemTouchListener(new BaseOnItemTouch(mPrivateChatUserList, new OnClickListener() {
            @Override
            public void onClick(RecyclerView.ViewHolder viewHolder) {
                mPrivateChatUserLayout.setVisibility(View.GONE);
                isPrivateChatUser = false;
                int position = mPrivateChatUserList.getChildAdapterPosition(viewHolder.itemView);
                PrivateUser privateUser = mPrivateUserAdapter.getPrivateUsers().get(position);
                privateUser.setRead(true);
                mPrivateUserAdapter.notifyDataSetChanged();
                if (isAllPrivateChatRead()) {
                    mPrivateIcon.setImageResource(R.mipmap.video_ic_private_msg_nor);
                }
                ChatEntity chatEntity = new ChatEntity();
                chatEntity.setUserId(privateUser.getId());
                chatEntity.setUserName(privateUser.getName());
                chatEntity.setUserAvatar(privateUser.getAvatar());
                click2PrivateChat(chatEntity, true);
            }
        }));

        mPrivateChatMsgList.setLayoutManager(new LinearLayoutManager(mContext));
        mPrivateChatAdapter = new PrivateChatAdapter(mContext);
        mPrivateChatMsgList.setAdapter(mPrivateChatAdapter);

        onSoftInputChange();
    }

    /**
     * 点击发起私聊
     */
    private void click2PrivateChat(ChatEntity chatEntity, boolean flag) {
        if (flag) { // 私聊用户列表点击发起私聊
            goPrivateChat(chatEntity);
            mCurPrivateUserId = chatEntity.getUserId();
        } else {
            if (!chatEntity.isPublisher()) { // 如果当前被点击的用户不是主播，则进行私聊
                hideKeyboard();
                mPrivateChatUserLayout.setVisibility(View.GONE);
                mPrivateIcon.setVisibility(View.GONE);
                goPrivateChat(chatEntity);
                mCurPrivateUserId = chatEntity.getUserId();
            }
        }
    }

    /**
     * 跳转私聊
     */
    private void goPrivateChat(ChatEntity chatEntity) {
        isPrivate = true;
        mTo = null;
        mTo = new ChatUser();
        mTo.setUserId(chatEntity.getUserId());
        mTo.setUserName(chatEntity.getUserName());
        ArrayList<ChatEntity> toChatEntitys = new ArrayList<>();
        for (ChatEntity entity : mPrivateChats) {
            // 从私聊列表里面读取到 当前发起私聊的俩个用户聊天列表
            if (entity.getUserId().equals(chatEntity.getUserId()) || entity.getReceiveUserId().equals(chatEntity.getUserId())) {
                toChatEntitys.add(entity);
            }
        }
        mPrivateChatAdapter.setDatas(toChatEntitys);
        showPrivateChatMsgList(chatEntity.getUserName());
    }

    /**
     * 判断是否所有私聊信息全部读完
     */
    private boolean isAllPrivateChatRead() {
        int i = 0;
        for (; i < mPrivateUserAdapter.getPrivateUsers().size(); i++) {
            if (!mPrivateUserAdapter.getPrivateUsers().get(i).isRead()) {
                break;
            }
        }
        return i >= mPrivateUserAdapter.getPrivateUsers().size();
    }

    private void onSoftInputChange() {
        mSoftKeyBoardState = new SoftKeyBoardState(mChatList, false);
        mSoftKeyBoardState.setOnSoftKeyBoardStateChangeListener(new SoftKeyBoardState.OnSoftKeyBoardStateChangeListener() {
            @Override
            public void onChange(boolean isShow) {
                isSoftInput = isShow;
                if (!isSoftInput) { // 软键盘隐藏
                    if (isEmoji) {
                        mEmojiGrid.setVisibility(View.VISIBLE);// 避免闪烁
                        isEmojiShow = true; // 修改emoji显示标记
                        isEmoji = false; // 重置
                    } else {
                        hideChatLayout(); // 隐藏聊天操作区域
                    }
                    if (isPrivateChatMsg && !isEmojiShow) { // 私聊软键盘隐藏时，显示公聊列表
                        mChatList.setVisibility(View.VISIBLE);
                    }
                } else {
                    hideEmoji();
                    if (isPrivateChatMsg) { // 私聊进行消息输入的时候隐藏公聊列表
                        mChatList.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @OnClick(R.id.iv_live_pc_private_chat)
    void openPrivateChatUserList() { // 显示私聊用户列表
        hideEmoji();
        hideKeyboard();
        showPrivateChatUserList();
    }

    @OnClick(R.id.id_private_chat_user_close)
    void closePrivateChatUserList() { // 关闭私聊用户列表
        hidePrivateChatUserList();
    }

    @OnClick(R.id.id_private_chat_close)
    void closePrivate() { // 关闭私聊
        hidePrivateChatMsgList();
    }

    @OnClick(R.id.id_push_chat_emoji)
    void emoji() {
        if (isEmojiShow) {
            hideEmoji();
            mInput.requestFocus();
            mInput.setSelection(mInput.getEditableText().length());
            mImm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            showEmoji();
        }
    }

    @OnClick(R.id.id_push_chat_send)
    void sendMsg() { // 发送聊天
        String msg = mInput.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) {
            toastOnUiThread(mContext, "聊天内容不能为空");
            return;
        }
        if (isPrivate) {
            DWLive.getInstance().sendPrivateChatMsg(mTo.getUserId(), msg);
        } else {
            DWLive.getInstance().sendPublicChatMsg(msg);
        }

        clearChatInput();
    }

    @OnClick(R.id.id_private_chat_back)
    void backChatUser() { // 返回私聊用户列表
        if (isSoftInput) {
            mImm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        }
        hidePrivateChatMsgList();
        showPrivateChatUserList();
    }

    void dismissAll() {
        if (isSoftInput) {
            mImm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        }
        hideChatLayout();
        hideEmoji();
        hidePrivateChatUserList();
        hidePrivateChatMsgList();
    }

    /**
     * 显示私聊用户列表
     */
    private void showPrivateChatUserList() {
        mChatLayout.setVisibility(View.GONE); // 隐藏聊天操作
        mPrivateIcon.setVisibility(View.GONE);
        mPrivateChatUserLayout.setVisibility(View.VISIBLE); // 显示用户列表
        isPrivateChatUser = true;
    }

    /**
     * 隐藏私聊用户
     */
    private void hidePrivateChatUserList() {
        if (isPrivateChatUser) {
            mChatLayout.setVisibility(View.VISIBLE);
            mPrivateIcon.setVisibility(View.VISIBLE);
            mPrivateChatUserLayout.setVisibility(View.GONE);
            isPrivateChatUser = false;
        }
    }

    public void hideChatLayout() {
        if (isChat) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(300L);
            mInput.setFocusableInTouchMode(false);
            mInput.clearFocus();
            mChatLayout.setVisibility(View.VISIBLE);
            isChat = false;
        }
    }

    /**
     * 显示emoji
     */
    public void showEmoji() {
        if (isSoftInput) {
            isEmoji = true; // 需要显示emoji
            mInput.clearFocus();
            mImm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        } else {
            mEmojiGrid.setVisibility(View.VISIBLE);// 避免闪烁
            isEmojiShow = true; // 修改emoji显示标记
        }
        mEmoji.setImageResource(R.drawable.push_chat_emoji);
    }

    /**
     * 隐藏emoji
     */
    public void hideEmoji() {
        if (isEmojiShow) { // 如果emoji显示
            mEmojiGrid.setVisibility(View.GONE);
            isEmojiShow = false; // 修改emoji显示标记
            mEmoji.setImageResource(R.drawable.push_chat_emoji_normal);
            if (!isSoftInput) {
                mChatList.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 显示私聊信息列表
     */
    public void showPrivateChatMsgList(final String username) {
        mChatLayout.setVisibility(View.VISIBLE);
        mInput.setFocusableInTouchMode(true);
        TranslateAnimation animation = new TranslateAnimation(1f, 1f, 0f, 1f);
        animation.setDuration(300L);
        mPrivateChatMsgLayout.startAnimation(animation);
        mPrivateChatMsgMask.setBackgroundColor(Color.parseColor("#FAFAFA"));
        mPrivateChatUserName.setText(username);
        mPrivateChatMsgLayout.setVisibility(View.VISIBLE);
        if (mPrivateChatAdapter.getItemCount() - 1 > 0) {
            mPrivateChatMsgList.smoothScrollToPosition(mPrivateChatAdapter.getItemCount() - 1);// 进行定位
        }
        isPrivateChatMsg = true;
    }

    /**
     * 隐藏私聊信息列表
     */
    public void hidePrivateChatMsgList() {
        if (isPrivateChatMsg) {
            hideEmoji();
            // 展示公聊区域和公聊内容
            mChatLayout.setVisibility(View.VISIBLE);
            mChatList.setVisibility(View.VISIBLE);
            mPrivateIcon.setVisibility(View.VISIBLE);
            mInput.setText("");
            mPrivateChatMsgMask.setBackgroundColor(Color.parseColor("#00000000"));
            mPrivateChatMsgLayout.setVisibility(View.GONE);
            isPrivateChatMsg = false;
            isPrivate = false;
        }
    }

    public void clearChatInput() {
        mInput.setText("");
        hideKeyboard();
    }

    public void hideKeyboard() {
        hideEmoji();
        mImm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
    }

    public void updatePrivateChat(ChatEntity chatEntity) {
        if (isPrivateChatMsg && (chatEntity.isPublisher() ||
                chatEntity.getUserId().equals(mCurPrivateUserId))) { // 如果当前界面是私聊信息界面直接在该界面进行数据更新
            mPrivateChatAdapter.add(chatEntity);
            mPrivateChatMsgList.smoothScrollToPosition(mPrivateChatAdapter.getItemCount() - 1);// 进行定位
        }
        PrivateUser privateUser = new PrivateUser();
        if (chatEntity.isPublisher()) {
            privateUser.setId(chatEntity.getReceiveUserId());
            privateUser.setName(chatEntity.getReceivedUserName());
            privateUser.setAvatar(chatEntity.getReceiveUserAvatar());
        } else {
            privateUser.setId(chatEntity.getUserId());
            privateUser.setName(chatEntity.getUserName());
            privateUser.setAvatar(chatEntity.getUserAvatar());
        }
        privateUser.setMsg(chatEntity.getMsg());
        privateUser.setTime(chatEntity.getTime());
        privateUser.setRead(isPrivateChatMsg && (chatEntity.isPublisher() ||
                chatEntity.getUserId().equals(mCurPrivateUserId)));
        mPrivateUserAdapter.add(privateUser);
        if (!isAllPrivateChatRead()) {
            mPrivateIcon.setImageResource(R.mipmap.video_ic_private_msg_new);
        }
        mPrivateChats.add(chatEntity);
    }

    public boolean onBackPressed() {
        if (isEmojiShow) {
            hideEmoji();
            hideChatLayout();
            return true;
        }
        if (isPrivateChatMsg) {
            hidePrivateChatMsgList();
            showPrivateChatUserList();
            return true;
        }
        if (isPrivateChatUser) {
            hidePrivateChatUserList();
            return true;
        }

        return false;
    }

    public void addChatEntity(ChatEntity chatEntity) {
        mChatAdapter.add(chatEntity);
        mChatList.smoothScrollToPosition(mChatAdapter.getItemCount() - 1);
    }

}
