package com.box.l10n.mojito.service.pollableTask;

import static com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage.Prefix.POLLABLE_TASK;

import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.service.blobstorage.Retention;
import com.box.l10n.mojito.service.blobstorage.StructuredBlobStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PollableTaskBlobStorage {

  @Autowired StructuredBlobStorage structuredBlobStorage;

  @Autowired
  @Qualifier("fail_on_unknown_properties_false")
  ObjectMapper objectMapper;

  public void saveInput(Long pollableTaskId, Object input) {
    String inputName = getInputName(pollableTaskId);
    String inputJson = objectMapper.writeValueAsStringUnchecked(input);
    structuredBlobStorage.put(POLLABLE_TASK, inputName, inputJson, Retention.MIN_1_DAY);
  }

  public void saveOutput(Long pollableTaskId, Object output) {
    String outputName = getOutputName(pollableTaskId);
    String outputJson = objectMapper.writeValueAsStringUnchecked(output);
    structuredBlobStorage.put(POLLABLE_TASK, outputName, outputJson, Retention.MIN_1_DAY);
  }

  public <T> T getInput(Long pollableTaskId, Class<T> clazz) {
    String inputName = getInputName(pollableTaskId);
    String inputJson =
        structuredBlobStorage
            .getString(POLLABLE_TASK, inputName)
            .orElseThrow(
                () -> new RuntimeException("Can't get the input json for: " + pollableTaskId));
    T t = objectMapper.readValueUnchecked(inputJson, clazz);
    return t;
  }

  public String getOutputJson(Long pollableTaskId) {
    String outputName = getOutputName(pollableTaskId);
    String outputJson =
        structuredBlobStorage
            .getString(POLLABLE_TASK, outputName)
            .orElseThrow(
                () -> new RuntimeException("Can't get the output json for: " + pollableTaskId));
    return outputJson;
  }

  public String getInputJson(Long pollableTaskId) {
    String inputName = getInputName(pollableTaskId);
    String inputJson =
        structuredBlobStorage
            .getString(POLLABLE_TASK, inputName)
            .orElseThrow(
                () -> new RuntimeException("Can't get the input json for: " + pollableTaskId));
    return inputJson;
  }

  public <T> T getOutput(Long pollableTaskId, Class<T> clazz) {
    String outputJson = getOutputJson(pollableTaskId);
    T t = objectMapper.readValueUnchecked(outputJson, clazz);
    return t;
  }

  String getInputName(long pollableTaskId) {
    return pollableTaskId + "/input";
  }

  String getOutputName(long pollableTaskId) {
    return pollableTaskId + "/output";
  }
}
