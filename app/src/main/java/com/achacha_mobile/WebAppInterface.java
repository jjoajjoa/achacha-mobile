package com.achacha_mobile;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void startNoti() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).showStartNoti(); // MainActivity의 showStartNoti 호출
        }
    }

    @JavascriptInterface
    public void endNoti() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).showEndNoti(); // MainActivity의 showStartNoti 호출
        }
    }

    @JavascriptInterface
    public void emergencyNoti() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).showEmergencyNoti(); // MainActivity의 showStartNoti 호출
        }
    }

}
