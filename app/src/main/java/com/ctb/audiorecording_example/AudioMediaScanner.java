package com.ctb.audiorecording_example;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

public class AudioMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private final MediaScannerConnection connection;
    private String path = "";
    public AudioMediaScanner(Context context){
        connection = new MediaScannerConnection(context, this);
    }

    public void start(String path){
        this.path = path;
        connection.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        if(path.isEmpty()) connection.scanFile(path, null);

    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        connection.disconnect();
        this.path = "";
    }
}
