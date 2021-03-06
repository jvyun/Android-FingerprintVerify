package com.yf.verify.callback;

import android.hardware.fingerprint.FingerprintManager;

/**
 * @author yangfeng
 * @create 2018/11/26 11:15
 * @Describe 指纹或密码验证的callback
 */
public interface FingerprintAuthenticatedCallback {
    void onFingerprintAuthenticatedSucceed(FingerprintManager.CryptoObject cryptoObject, boolean withFingerprint);

    void onFingerprintAuthenticatedSucceed(String passWord, InputPassWordCallback passWordCallback);
}
