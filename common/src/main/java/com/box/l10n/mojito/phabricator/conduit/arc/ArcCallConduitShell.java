package com.box.l10n.mojito.phabricator.conduit.arc;

import com.box.l10n.mojito.phabricator.conduit.Method;
import com.box.l10n.mojito.phabricator.conduit.payload.Constraints;
import com.box.l10n.mojito.phabricator.conduit.payload.ResponseWithError;
import com.box.l10n.mojito.shell.Result;
import com.box.l10n.mojito.shell.Shell;
import com.box.l10n.mojito.json.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ArcCallConduitShell {

    Shell shell;
    ObjectMapper objectMapper;

    public ArcCallConduitShell(Shell shell) {
        this.shell = shell;
        initObjectMapper();
    }

    void initObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
    }

    public <T extends ResponseWithError> T callConduit(Method method, Constraints constrains, Class<T> clazz) {
        String command = getCommand(method, constrains);
        String commandOutput = callShell(command);
        T t = objectMapper.readValueUnchecked(commandOutput, clazz);
        return t;
    }

    String callShell(String command) {

        Result result = shell.exec(command);

        if (result.getExitCode() != 0) {
            throw new RuntimeException("Shell call to conduit failed");
        }

        return result.getOutput();
    }

    String getCommand(Method method, Constraints constraints) {
        String jsonConstraints = objectMapper.writeValueAsStringUnchecked(constraints);
        return "echo '" + jsonConstraints + "' | arc call-conduit " + method.getMethod();
    }
}
