package com.achacha_mobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    // 데이터 영구 저장
    @JavascriptInterface
    public void saveCredentials(String employeeUniqueNumber, String employeePassword) {
        Log.d("app", "로그인 함수 실행됨");
        // 로그인 정보를 SharedPreferences에 저장
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", employeeUniqueNumber);
        editor.putString("password", employeePassword);
        editor.apply();

        Log.d("userId", "저장된 userId: " + employeeUniqueNumber);

        ((MainActivity) mContext).fetchFCMToken();
        // 필요하다면 서버에 로그인 정보를 전송하는 메서드를 호출할 수 있습니다. - 자동로그인
        // sendLoginToServer(username, password);
    }

    @JavascriptInterface
    public void logout() {
        // SharedPreferences에서 로그인 정보 삭제
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 로그인 정보 삭제
        editor.remove("userId");  // 사용자 ID 삭제
        editor.remove("password");  // 비밀번호 삭제
        editor.apply();  // 변경 사항 적용

        // 추가적으로, 서버에서 로그아웃 처리를 한다면, 이곳에서 서버 요청을 할 수 있음
        // sendLogoutToServer();  // 서버에 로그아웃 요청 보내기
    }

    @JavascriptInterface
    public void emergencyNoti() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).showEmergencyNoti(); // MainActivity의 showStartNoti 호출
        }
    }

}
