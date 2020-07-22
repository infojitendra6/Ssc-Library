package com.kopykitab.ereader.settings;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileService extends IntentService {
    public static final int UPDATE_PROGRESS = 8340;
    public static final int FINISH_PROGRESS = 8342;
    public static final int ERROR_PROGRESS = 8344;
    public static final int TERMINATE_DOWNLOADING = 8346;
    public static final int CANCEL_BEFORE_DOWNLOAD_START = 8348;

    public static volatile boolean isCancelled = false;
    private File outputFile = null;

    public DownloadFileService() {
        super("DownloadFileService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;

        String productId = intent.getStringExtra("productId");
        String customerId = intent.getStringExtra("customerId");
        String pdfUrl = intent.getStringExtra("url");
//
        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        try {
            URL url = new URL(pdfUrl);
            connection = (HttpURLConnection) url.openConnection();
            try {
                PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + packageInfo.packageName + "/content/" + packageInfo.versionName + "]");
            } catch (Exception e) {
                e.printStackTrace();
                connection.setRequestProperty("User-Agent", System.getProperty("http.agent") + " [" + getApplicationContext().getPackageName() + "/content]");
            }
            connection.connect();

            // always expect HTTP 200 OK
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                String response = "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                Utils.triggerGAEvent(getApplicationContext(), "Pdf_" + connection.getResponseCode(), productId, customerId);
                Bundle resultData = new Bundle();
                resultData.putString("pdfUrl", pdfUrl);
                resultData.putString("response", response);
                receiver.send(ERROR_PROGRESS, resultData);

                return;
            }

            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            // download the file
            Utils.triggerGAEvent(getApplicationContext(), "Pdf_Download_Started", productId, customerId);
            input = connection.getInputStream();
            outputFile = new File(Utils.getFileDownloadPath(getApplicationContext(), pdfUrl));
            output = new FileOutputStream(outputFile);

            if (isCancelled) {
                outputFile.delete();
                Bundle resultData = new Bundle();
                receiver.send(CANCEL_BEFORE_DOWNLOAD_START, resultData);

                return;
            }

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled) {
                    input.close();
                    outputFile.delete();

                    Bundle resultData = new Bundle();
                    receiver.send(TERMINATE_DOWNLOADING, resultData);

                    return;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) { // only if total length is known
                    Bundle resultData = new Bundle();
                    resultData.putInt("progress", (int) (total * 100 / fileLength));
                    receiver.send(UPDATE_PROGRESS, resultData);
                }
                output.write(data, 0, count);
            }
        } catch (IOException e) {
            Bundle resultData = new Bundle();
            resultData.putString("pdfUrl", pdfUrl);
            resultData.putString("response", e.toString());
            receiver.send(ERROR_PROGRESS, resultData);

            return;
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException ie) {
                ie.printStackTrace();
            }

            if (connection != null)
                connection.disconnect();
        }

        Bundle resultData = new Bundle();
        receiver.send(FINISH_PROGRESS, resultData);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
    }
}