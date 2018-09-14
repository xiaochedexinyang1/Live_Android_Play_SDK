package com.bokecc.dwlivedemo_new.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

//-----------------------布局区域控制基类----------------------------

public class BaseLayoutController {

    /**
     * 在UI线程上进行吐司提示
     */
    public void toastOnUiThread(final Context context, final String msg) {
        // 判断是否处在UI线程
        if (!checkOnMainThread()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    showToast(context, msg);
                }
            });
        } else {
            showToast(context, msg);
        }
    }

    /**
     * 判断当前的线程是否是UI线程
     */
    private boolean checkOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }


    /**
     * 进行吐司提示
     */
    private void showToast(Context context, String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
