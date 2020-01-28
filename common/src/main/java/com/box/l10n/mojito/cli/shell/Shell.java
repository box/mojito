package com.box.l10n.mojito.cli.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Shell {

    static Logger logger = LoggerFactory.getLogger(Shell.class);

    public Result exec(String... command) {
        try {
            List<String> commandWithShell = new ArrayList<>();
            commandWithShell.add("sh");
            commandWithShell.add("-c");
            commandWithShell.addAll(Arrays.asList(command));

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(commandWithShell);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();

            StreamGobbler streamGobblerOutput = new StreamGobbler(process.getInputStream(), output::append);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), error::append);

            Thread threadInput = new Thread(streamGobblerOutput);
            Thread threadOutput = new Thread(streamGobblerError);

            threadInput.start();
            threadOutput.start();

            process.waitFor();
            threadInput.join();
            threadOutput.join();

            return new Result(process.exitValue(), output.toString(), error.toString());
        } catch (Exception e) {
            throw new RuntimeException("Can't run shell", e);
        }
    }

}
