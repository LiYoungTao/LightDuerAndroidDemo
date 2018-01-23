package com.baidu.lightduer.sample;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.lightduer.lib.androidsystemimpl.audioinput.CustomAudioRecord;
import com.baidu.lightduer.lib.androidsystemimpl.player.LightDuerMediaPlayer;
import com.baidu.lightduer.lib.api.ClientImpl;
import com.baidu.lightduer.lib.api.DuerLightOSSDK;
import com.baidu.lightduer.lib.framework.LightClient;
import com.baidu.lightduer.lib.jni.LightduerOsJni;
import com.baidu.lightduer.lib.jni.controlpoint.LightduerResource;
import com.baidu.lightduer.lib.jni.controlpoint.LightduerResourceListener;
import com.baidu.lightduer.lib.jni.event.LightduerEvent;
import com.baidu.lightduer.lib.jni.event.LightduerEventId;
import com.baidu.lightduer.lib.jni.utils.LightduerAddress;
import com.baidu.lightduer.lib.jni.utils.LightduerContext;
import com.baidu.lightduer.lib.jni.utils.LightduerMessage;
import com.baidu.lightduer.lib.util.FileUtil;
import com.baidu.lightduer.lib.util.NetWorkUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.baidu.lightduer.lib.jni.LightduerOsJni.DCS_NEXT_CMD;
import static com.baidu.lightduer.lib.jni.LightduerOsJni.DCS_PREVIOUS_CMD;

/**
 * 主界面 activity
 * <p>
 *
 */
public class LightDuerMainActivity extends Activity {
    public static final String TAG = "LightDuerMainActivity";
    public static final boolean IS_USE_CUSTOM_AUDIO = true;
    private Button voiceButton, preButton, nextButton;
    private TextView resultTv;
    private LightDuerMediaPlayer mMediaPlayer; // 媒体播放器
    private CustomAudioRecord mCustomAudioRecord;
    StringBuilder profileSB;
    int mcount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaPlayer = new LightDuerMediaPlayer();
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        //初始化视图
        initView();
        //3.初始化监听器
        initListener();
        profileSB = new StringBuilder();
        FileUtil.readFileToStringBuilder(FileUtil.getProfilePath(), profileSB);
        //1.初始化SDK
        DuerLightOSSDK.getInstance().getClient().init();
        if (!TextUtils.isEmpty(profileSB)) {
            resultTv.setText(profileSB);
            //2.连接server
            DuerLightOSSDK.getInstance().getClient().connectServer(profileSB.toString(), iConnectStatusListener);

        } else {
            resultTv.setText(R.string.profile_read_error);
        }
        if (IS_USE_CUSTOM_AUDIO) {
            mCustomAudioRecord = DuerLightOSSDK.getInstance().getClient().getCustomAudioRecord();
        }
    }

    private LightDuerMediaPlayer.OnCompletionListener mOnCompletionListener = new LightDuerMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(int type) {
            switch (type) {
                case LightDuerMediaPlayer.TYPE_TTS:
                    DuerLightOSSDK.getInstance().getClient().reportPlayState(ClientImpl.PlayStatus.TTS_PLAY_END);
                    break;
                case LightDuerMediaPlayer.TYPE_AUDIO:
                    DuerLightOSSDK.getInstance().getClient().reportPlayState(ClientImpl.PlayStatus.AUDIO_PLAY_END);

                    break;
            }
        }
    };

    LightClient.IConnectStatusListener iConnectStatusListener = new LightClient.IConnectStatusListener() {
        @Override
        public void onConnectStatus(LightduerEvent lightduerEvent) {
            if (lightduerEvent.getEventid() == LightduerEventId.DUER_EVENT_STARTED) {
                Toast.makeText(LightDuerMainActivity.this, R.string.connect_success, Toast.LENGTH_LONG).show();
                addPointControl();
            } else {
                if (NetWorkUtil.isNetworkConnected(LightDuerMainActivity.this) && !TextUtils.isEmpty(profileSB)) {
                    DuerLightOSSDK.getInstance().getClient().connectServer(profileSB.toString(), iConnectStatusListener);
                }
                Toast.makeText(LightDuerMainActivity.this, R.string.connect_fail, Toast.LENGTH_LONG).show();
            }
        }
    };

    private void initListener() {
        //3.1语音输入监听，开始录音和结束录音的状态
        DuerLightOSSDK.getInstance().getClient().setAudioInputListener(new LightClient.IAudioInputListener() {
            @Override
            public void onStartRecord() {
                Log.d(TAG, "onStartRecord");
                startRecording();
                mMediaPlayer.stop();
            }
            @Override
            public void onStopRecord() {
                Log.d(TAG, "onStopRecord");
                stopRecording();
            }
            @Override
            public void onErrorRecord(int errorCode) {
                Log.d(TAG, "record error code = " + errorCode);
                if (ClientImpl.ERROR_AUDIO_NO_PERMISSION == errorCode) {
                    ActivityCompat.requestPermissions(LightDuerMainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        });
        //3.2speak（tts）返回结果监听
        DuerLightOSSDK.getInstance().getClient().setVoiceOutputListener(new LightClient.VoiceOutputListener() {
            @Override
            public int speak(String url) {
                resultTv.setText("(VoiceOutputListener)url = " + url);
                mMediaPlayer.playUrl(url, LightDuerMediaPlayer.TYPE_TTS);
                return 0;
            }
        });
        //3.3Audio结果返回监听
        DuerLightOSSDK.getInstance().getClient().setAudioPlayerListener(new LightClient.AudioPlayerListener() {
            @Override
            public int play(String url) {
                resultTv.setText("(AudioPlayerListener)url = " + url);
                mMediaPlayer.playUrl(url, LightDuerMediaPlayer.TYPE_AUDIO);
                return 0;
            }
            @Override
            public int stop() {
                return 0;
            }
            @Override
            public int resume(String url, int offset) {
                return 0;
            }
            @Override
            public int pause() {
                return 0;
            }
            @Override
            public int getAudioPlayProgress() {
                return 0;
            }
        });
    }

    private void initView() {
        voiceButton = findViewById(R.id.id_btn_voice);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!NetWorkUtil.isNetworkConnected(LightDuerMainActivity.this)) {
                    Toast.makeText(LightDuerMainActivity.this,
                            getResources().getString(R.string.err_net_msg),
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (IS_USE_CUSTOM_AUDIO) {
                        if (CustomAudioRecord.Status.STOPED == mCustomAudioRecord.getAudioStatus()) {
                            mCustomAudioRecord.startRecord(16000);
                            sendDataToCustomAudioData();
                        }
                    } else {
                        DuerLightOSSDK.getInstance().getClient().startRecord();
                    }
                }
            }
        });

        preButton = findViewById(R.id.id_btn_PreAudio);
        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LightduerOsJni.sendPlayControlCommand(DCS_PREVIOUS_CMD);
            }
        });

        nextButton = findViewById(R.id.id_btn_NextAudio);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LightduerOsJni.sendPlayControlCommand(DCS_NEXT_CMD);
            }
        });
        resultTv = findViewById(R.id.id_tv_VoiceResultText);
    }

    private void stopRecording() {
        voiceButton.setText(getResources().getString(R.string.start_record));
    }

    private void startRecording() {
        voiceButton.setText(getResources().getString(R.string.stop_record));
    }

    /**
     * just use for CustomAudio,sendData to DuerOS
     * mian interface:DuerLightOSSDK.getInstance().getClient().getCustomAudioRecord().sendVoiceData(buffer);
     * 传送录音给服务器
     */
    private void sendDataToCustomAudioData() {
        BufferedInputStream in = null;
        BufferedInputStream in2 = null;
        FileInputStream stream = null;
                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/reverseme.pcm");

        try {
          stream = new FileInputStream(file);
            in = new BufferedInputStream(stream);
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            while (-1 != (in.read(buffer, 0, buf_size))) {
                DuerLightOSSDK.getInstance().getClient().getCustomAudioRecord().sendVoiceData(buffer);
            }
            byte[] buffer2 = new byte[buf_size];
            in2 = new BufferedInputStream(getAssets().open("white.pcm"));
            while (-1 != (in2.read(buffer2, 0, buf_size))) {
                DuerLightOSSDK.getInstance().getClient().getCustomAudioRecord().sendVoiceData(buffer2);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
                in.close();
                in2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //4.添加数据点控
    private void addPointControl() {
        LightduerResource[] resources = new LightduerResource[2];
        resources[0] = new LightduerResource(
                LightduerResource.DUER_RES_MODE_DYNAMIC, LightduerResource.DUER_RES_OP_PUT | LightduerResource.DUER_RES_OP_GET,
                "volume", null, new LightduerResourceListener() {
            @Override
            public int callback(LightduerContext context, LightduerMessage message, LightduerAddress address) {
                Log.i(TAG, "test control point");
                String p = new String(message.getPayload());
                Log.i(TAG, "payload: " + p);
                DuerLightOSSDK.getInstance().getClient().sendResponse(message, LightduerMessage.DUER_MSG_RSP_CHANGED, "baidu".getBytes());
                return 0;
            }
        });
        String static_resource = "BAIDU";
        resources[1] = new LightduerResource(
                LightduerResource.DUER_RES_MODE_STATIC, LightduerResource.DUER_RES_OP_PUT | LightduerResource.DUER_RES_OP_GET,
                "test2", static_resource.getBytes(), new LightduerResourceListener() {
            @Override
            public int callback(LightduerContext context, LightduerMessage message, LightduerAddress address) {
                Log.i(TAG, "test control point");
                String p = new String(message.getPayload());
                Log.i(TAG, "payload: " + p);
                DuerLightOSSDK.getInstance().getClient().sendResponse(message, LightduerMessage.DUER_MSG_RSP_CHANGED, "baidu".getBytes());
                return 0;
            }
        });
        DuerLightOSSDK.getInstance().getClient().addControlPoint(resources);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DuerLightOSSDK.getInstance().getClient().release();
        mMediaPlayer.release();
    }
}
