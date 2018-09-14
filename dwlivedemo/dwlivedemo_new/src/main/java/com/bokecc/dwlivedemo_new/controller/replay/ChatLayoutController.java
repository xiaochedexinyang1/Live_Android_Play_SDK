package com.bokecc.dwlivedemo_new.controller.replay;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bokecc.dwlivedemo_new.DWApplication;
import com.bokecc.dwlivedemo_new.R;
import com.bokecc.dwlivedemo_new.adapter.LivePublicChatAdapter;
import com.bokecc.dwlivedemo_new.controller.BaseLayoutController;
import com.bokecc.dwlivedemo_new.module.ChatEntity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * 回放聊天布局区域控制类
 */
public class ChatLayoutController extends BaseLayoutController {

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

    public ChatLayoutController(Context context, View view) {
        mContext = context;
        ButterKnife.bind(this, view);
        mChatInfoLength = 0;
        mChatLayout.setVisibility(View.GONE);
        mPrivateChatIcon.setVisibility(View.GONE);
    }

    LivePublicChatAdapter mChatAdapter;

    public void initChat() {
        mChatList.setLayoutManager(new LinearLayoutManager(mContext));
        mChatAdapter = new LivePublicChatAdapter(mContext);
        mChatList.setAdapter(mChatAdapter);
    }


    public boolean onBackPressed() {
        return false;
    }

    /**
     * 回放的聊天添加
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
