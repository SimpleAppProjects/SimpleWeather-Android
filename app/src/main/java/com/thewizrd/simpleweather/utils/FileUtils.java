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
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = reader.readLine();
        StringBuilder sBuilder = new StringBuilder();

        while (line != null) {
            sBuilder.append(line).append("\n");
            line = reader.readLine();
        }

        String data = sBuilder.toString();

        return data;
    }

    public static void writeToFile(String data, File file) throws IOException {
        FileOutputStream outputStream;

        outputStream = App.getAppContext().openFileOutput(file.getName(), Context.MODE_PRIVATE);
        outputStream.write(data.getBytes());
        outputStream.close();
    }
}
