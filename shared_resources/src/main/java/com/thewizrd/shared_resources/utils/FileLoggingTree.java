package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();

    private Context context;

    public FileLoggingTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {

        try {

            File direct = new File(context.getExternalFilesDir(null) + "/logs");

            if (!direct.exists()) {
                direct.mkdir();
            }

            String dateTimeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
            String logTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.ROOT).format(new Date());

            String fileName = "Logger." + dateTimeStamp + ".log";

            File file = new File(direct.getPath() + File.separator + fileName);

            if (!file.exists())
                file.createNewFile();

            if (file.exists()) {

                OutputStream fileOutputStream = new FileOutputStream(file, true);

                String priorityTAG = null;
                switch (priority) {
                    default:
                    case Log.DEBUG:
                        priorityTAG = "DEBUG";
                        break;
                    case Log.INFO:
                        priorityTAG = "INFO";
                        break;
                    case Log.VERBOSE:
                        priorityTAG = "VERBOSE";
                        break;
                    case Log.WARN:
                        priorityTAG = "WARN";
                        break;
                    case Log.ERROR:
                        priorityTAG = "ERROR";
                        break;
                }

                if (t != null)
                    fileOutputStream.write((logTimeStamp + "|" + priorityTAG + "|" + (tag == null ? "" : tag + "|") + message + "\n" + t.toString() + "\n").getBytes());
                else
                    fileOutputStream.write((logTimeStamp + "|" + priorityTAG + "|" + (tag == null ? "" : tag + "|") + message + "\n").getBytes());

                fileOutputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while logging into file : " + e);
        }
    }
}
