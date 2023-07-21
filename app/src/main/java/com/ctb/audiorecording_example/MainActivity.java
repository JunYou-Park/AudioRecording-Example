package com.ctb.audiorecording_example;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnRecord, btnPlay, btnSave;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private AudioRecord audioRecord = null;
    private AudioTrack audioTrack = null;
    private File audioFile = null;
    private FileOutputStream fos;
    int minimumBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecord = findViewById(R.id.btn_record);
        btnRecord.setOnClickListener(v->{
            if(v.getTag().toString().equals("stop")){
                startRecord();
            }
            else {
                releaseRecord();
            }
        });

        btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v->{

        });
    }
    private void play(){
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLING_RATE_IN_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(minimumBufferSize)
                .build();
    }

    private void startRecord(){
        if(minimumBufferSize < AudioRecord.SUCCESS) {
            Toast.makeText(this, "잘못된 크기: " + minimumBufferSize, Toast.LENGTH_SHORT).show();
            return;
        }
        if(audioRecord==null){
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minimumBufferSize);
        }
        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED){
            btnRecord.setText("정지");
            btnRecord.setTag("record");
            if(audioFile == null) audioFile = createNewAudioFile();
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[minimumBufferSize];
                    try {
                        fos = new FileOutputStream(audioFile);
                        audioRecord.startRecording();
                        while (btnRecord.getTag().toString().contentEquals("recording")){
                            audioRecord.read(buffer, 0, minimumBufferSize);
                            fos.write(buffer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopRecording();
                    }
                }
            });
        }
        else{
            Toast.makeText(this, "잘못된 상태:" + audioRecord.getState(), Toast.LENGTH_SHORT).show();
            stopRecording();
        }
    }
    private File createNewAudioFile(){
        String path = Environment.getExternalStorageDirectory().toString() + "/Download/";
        String fileName = "AUDIO_" + System.currentTimeMillis();
        return new File(path, fileName + ".pcm");
    }

    private void stopRecording(){
        releaseRecord();
        audioFile = null;
    }

    private void releaseRecord(){
        btnRecord.setTag("pause");
        btnRecord.setText("녹음");
        if(audioRecord!=null && audioRecord.getState() != AudioRecord.STATE_UNINITIALIZED){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if(fos != null){
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos = null;
        }
    }

}