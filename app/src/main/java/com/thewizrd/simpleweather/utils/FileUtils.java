package com.thewizrd.simpleweather.utils;

import android.content.Context;

import com.thewizrd.simpleweather.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtils {
    public static String readFile(File file) throws IOException {
        while (isFileLocked(file))
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = reader.readLine();
        StringBuilder sBuilder = new StringBuilder();

        while (line != null) {
            sBuilder.append(line).append("\n");
            line = reader.readLine();
        }

        String data = sBuilder.toString();

        // Close stream
        reader.close();

        return data;
    }

    public static void writeToFile(String data, File file) throws IOException {
        while (isFileLocked(file))
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream outputStream;

        outputStream = App.getAppContext().openFileOutput(file.getName(), Context.MODE_PRIVATE);
        outputStream.write(data.getBytes());
        outputStream.close();
    }

    public static boolean isFileLocked(File file) {
        if (!file.exists())
            return false;

        FileInputStream stream = null;

        try {
            stream = new FileInputStream(file);
        }
        catch (IOException e)
        {
            //the file is unavailable because it is:
            //still being written to
            //or being processed by another thread
            //or does not exist (has already been processed)
            return true;
        }
        finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        //file is not locked
        return false;
    }
}
