package com.bokecc.dwlivedemo_new.base.contract;

import com.bokecc.dwlivedemo_new.base.TitleOptions;

/**
 * 作者 ${郭鹏飞}.<br/>
 */

public interface TitleContract {
    interface View {
        /**
         * 进行标题设置
         * @param options 标题配置参数
         */
        void setTitleOptions(TitleOptions options);
    }
}
