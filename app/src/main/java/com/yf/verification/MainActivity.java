package com.yf.verification;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.yf.verify.callback.FingerprintAuthenticatedCallback;
import com.yf.verify.callback.CodedLockAuthenticatedCallBack;
import com.yf.verify.codedlock.CodedLockCharacter;
import com.yf.verify.codedlock.CodedLockAuthenticatedStepBuilder;
import com.yf.verify.fingerprint.FingerprintCharacter;
import com.yf.verify.fingerprint.FingerprintCharacterStepBuilder;
import com.yf.verify.callback.InputPassWordCallback;
import com.yf.verify.util.LogUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends AppCompatActivity implements FingerprintAuthenticatedCallback, CodedLockAuthenticatedCallBack {
    private FingerprintCharacter fingerprintAuthenticatedCharacter;
    private CodedLockCharacter codedLockAuthenticatedCharacter;
    private BiometricPrompt mBiometricPrompt;
    private CancellationSignal mCancellationSignal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button purchaseButton = findViewById(R.id.purchase_button);
        Button passwordButton = findViewById(R.id.password_button);
        //初始化指纹验证
        fingerprintAuthenticatedCharacter = FingerprintCharacterStepBuilder
                .newBuilder()
                .setKeystoreAlias("key1")
                .setDialogTag(FingerprintCharacterStepBuilder.DIALOG_FRAGMENT_TAG)
                .setFingerprintCallback(this)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            mBiometricPrompt = new BiometricPrompt.Builder(MainActivity.this).setDescription("支付金额").setNegativeButton("取消支付", getMainExecutor(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.e("yyyy", "DialogInterface");
                }
            }).setSubtitle("用户进行支付").setTitle("支付").build();
        }

        //初始化密码验证
        codedLockAuthenticatedCharacter = CodedLockAuthenticatedStepBuilder
                .newBuilder()
                .setActivity(MainActivity.this)
                .getKeyguardManager()
                .setKeystoreAlias("my_key")
                .setUserAuthenticationValidityDurationSeconds(10)
                .getKeyStore()
                .setAuthenticationScreenCallBack(MainActivity.this)
                .build();

        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mCancellationSignal = new CancellationSignal();
                    mBiometricPrompt.authenticate(mCancellationSignal, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            mCancellationSignal.cancel();
                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                            super.onAuthenticationHelp(helpCode, helpString);
                        }

                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            mCancellationSignal.cancel();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                        }
                    });
                } else {
                    fingerprintAuthenticatedCharacter.show(getSupportFragmentManager());
                }
            }
        });
        passwordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (codedLockAuthenticatedCharacter.isKeyguardSecure()) {
                    codedLockAuthenticatedCharacter.onValidate();
                }
            }
        });
    }


    @Override
    public void onFingerprintAuthenticatedSucceed(FingerprintManager.CryptoObject cryptoObject, boolean withFingerprint) {
        onPurchased( /* withFingerprint */ withFingerprint, cryptoObject);
    }

    public void onPurchased(boolean withFingerprint,
                            @Nullable FingerprintManager.CryptoObject cryptoObject) {
        if (withFingerprint) {
            // 如果用户已通过指纹验证，请使用密码学和验证
            //然后显示确认信息。
            assert cryptoObject != null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tryEncrypt(cryptoObject.getCipher());
            }
        }
    }

    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal("Very secret message".getBytes());
            LogUtils.e("yyyy", encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {

        }
    }

    /**
     * 密码验证activity跳转回传的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CodedLockCharacter.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                if (codedLockAuthenticatedCharacter.onValidate()) {
                    LogUtils.e("yyy", "密码验证成功");
                } else {
                    LogUtils.e("yyy", "密码验证失败");
                }
            } else {
                //用户取消或没有完成锁定屏幕
                Toast.makeText(this, "用户取消或没有完成锁定屏幕", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * @param passWord
     * @param passWordCallback
     */
    @Override
    public void onFingerprintAuthenticatedSucceed(String passWord, InputPassWordCallback passWordCallback) {
        if ("1234".equals(passWord)) {//成功后，调用成功的方法，在dialog中，可以让dialog关闭
            if (null != passWordCallback) {
                passWordCallback.onInputSucceed();
            }
        } else {
            if (null != passWordCallback) {//失败后，调用失败的方法，在dialog中，可以弹出toast，如果想自己定义，可以不调用次方法
                passWordCallback.onInputFailed();
            }
        }
    }

    /**
     * 密码锁验证失败成功
     */
    @Override
    public void onCodedLockAuthenticationFailed() {
        LogUtils.e("yyy", "密码验证失败");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != codedLockAuthenticatedCharacter) {
            codedLockAuthenticatedCharacter.onDestroy();
        }
    }
}
