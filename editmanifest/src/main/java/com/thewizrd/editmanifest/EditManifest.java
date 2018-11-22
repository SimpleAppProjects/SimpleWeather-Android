package com.thewizrd.editmanifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class EditManifest {
    public static void main(String args[]) {
        String configMode = "release";
        String filePath = "";

        if (args == null || args.length < 1) {
            System.out.println("Missing arguments!!");
            System.exit(1);
        } else {
            filePath = args[0];

            if (args.length > 1) {
                configMode = args[1];
            }

            System.out.println(String.format("File path: %s", filePath));
            System.out.println(String.format("Config mode: %s", configMode));

            OutputStreamWriter sWriter = null;
            BufferedReader sReader = null;

            try {
                StringBuilder sBuilder = new StringBuilder();
                File file = new File(filePath);
                sReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

                String line = sReader.readLine();

                while (line != null) {
                    if ("debug".equals(configMode)) {
                        if (!line.contains("com.thewizrd.simpleweather.debug"))
                            line = line.replace("com.thewizrd.simpleweather.", "com.thewizrd.simpleweather.debug.");
                    } else {
                        line = line.replace("com.thewizrd.simpleweather.debug.", "com.thewizrd.simpleweather.");
                    }

                    sBuilder.append(line).append(System.lineSeparator());
                    line = sReader.readLine();
                }

                sWriter = new OutputStreamWriter(new FileOutputStream(file, false));
                sWriter.write(sBuilder.toString());
                sWriter.flush();
            } catch (FileNotFoundException fNE) {
                System.out.println(String.format("File \"%s\" does not exist!!", filePath));
                System.exit(1);
            } catch (Exception e) {
                System.out.println("Error accessing file!!");
                System.exit(1);
            } finally {
                if (sWriter != null) {
                    try {
                        sWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (sReader != null) {
                    try {
                        sReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
