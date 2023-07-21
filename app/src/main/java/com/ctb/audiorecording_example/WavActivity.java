package com.ctb.audiorecording_example;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WavActivity extends AppCompatActivity {

    private Button btnRecording;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private AudioRecord audioRecord = null;
    private AudioTrack audioTrack = null;
    private FileOutputStream fos;
    private File audioFile = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRecording = findViewById(R.id.btn_record);
        btnRecording.setOnClickListener(v->{
            if(v.getTag().toString().equals("stop")){
                startRecording();
            }
            else {
                stopRecording();
            }
        });
    }

    // T = (AvailableStorage / (SampleRate * BitsPerSample * NumberOfChannels)) / 3600

    private void startRecording(){
        int minimumBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if(minimumBufferSize < AudioRecord.SUCCESS) {
            Toast.makeText(this, "잘못된 크기: " + minimumBufferSize, Toast.LENGTH_SHORT).show();
            return;
        }
        if(audioRecord==null){
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE_IN_HZ, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minimumBufferSize);
        }
        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED){
            btnRecording.setText("중지");
            btnRecording.setTag("recording");
            audioFile = createNewAudioFile();
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[minimumBufferSize];
                    try {
                        fos = new FileOutputStream(audioFile);
                        audioRecord.startRecording();
                        while (btnRecording.getTag().toString().contentEquals("recording")){
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
        return new File(path, fileName + ".wav");
    }

    private void stopRecording(){
        Log.d("test", "stopRecording: audioFile parent=" + audioFile.getParent());
        btnRecording.setTag("stop");
        btnRecording.setText("녹음");
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
        makeToWavFile(audioFile);
    }

    private File copyFile(File file){
        String path = Environment.getExternalStorageDirectory() + "/Download/";
        File newFile = new File(path + "Audio_" + System.currentTimeMillis() + ".wav");
        try {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(newFile);
            int readBuffer = 0;
            byte[] buffer = new byte[(int)file.length()];
            while ((readBuffer = fis.read(buffer)) != -1){
                fos.write(buffer, 0, readBuffer);
            }
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newFile;

    }

    private void makeToWavFile(File file){
        long totalAudioLength = file.length();
        RandomAccessFile wavFile;
        try {
            wavFile = new RandomAccessFile(file, "rw");
            wavFile.seek(0); // to the beginning
            wavFile.write(wavFileHeader(totalAudioLength - 44,
                    totalAudioLength - 44 + 36,
                    SAMPLING_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_STEREO,
                    16 * SAMPLING_RATE_IN_HZ * AudioFormat.CHANNEL_IN_STEREO / 8,
                    (byte)16));
            wavFile.close();
            AudioMediaScanner mediaScanner = new AudioMediaScanner(this);
            mediaScanner.start(file.getPath());

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private byte[] wavFileHeader(long totalAudioLen, long totalDataLen, long longSampleRate,
                                 int channels, long byteRate, byte bitsPerSample) {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * (bitsPerSample / 8)); //
        // block align
        header[33] = 0;
        header[34] = bitsPerSample; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }
}