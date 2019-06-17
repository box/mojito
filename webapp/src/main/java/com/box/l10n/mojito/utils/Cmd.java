package com.box.l10n.mojito.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Cmd {

    public static String getOutputIfSuccess(String command) throws IOException, InterruptedException {
        Process process = java.lang.Runtime.getRuntime().exec(String.format(command));
        String output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.joining("\n"));
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Command must succeed:" + command);
        }

        return output;
    }
}
