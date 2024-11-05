package com.achacha_mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void saveCredentials(String employeeUniqueNumber, String employeePassword) {
        // 로그인 정보를 SharedPreferences에 저장
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", employeeUniqueNumber);
        editor.putString("password", employeePassword);
        editor.apply();

        ((MainActivity) mContext).fetchFCMToken();
        // 필요하다면 서버에 로그인 정보를 전송하는 메서드를 호출할 수 있습니다. - 자동로그인
        // sendLoginToServer(username, password);
    }

    @JavascriptInterface
    public void emergencyNoti() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).showEmergencyNoti(); // MainActivity의 showStartNoti 호출
        }
    }

}
