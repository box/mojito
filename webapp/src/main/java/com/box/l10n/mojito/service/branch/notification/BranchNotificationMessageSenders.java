package com.box.l10n.mojito.service.branch.notification;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.box.l10n.mojito.service.branch.notification.BranchNotificationMessageSendersConfigurationProperties.NoopConfigurationProperties;
import com.box.l10n.mojito.service.branch.notification.noop.BranchNotificationMessageSenderNoop;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BranchNotificationMessageSenders {

  final Map<String, BranchNotificationMessageSender> mapIdToBranchNotificationMessageSender;

  public BranchNotificationMessageSenders(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      BranchUrlBuilder branchUrlBuilder) {
    this.mapIdToBranchNotificationMessageSender =
        createInstancesFromConfiguration(
            branchNotificationMessageSendersConfigurationProperties, branchUrlBuilder);
  }

  public BranchNotificationMessageSender getById(String id) {
    return mapIdToBranchNotificationMessageSender.get(id);
  }

  private Map<String, BranchNotificationMessageSender> createInstancesFromConfiguration(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties,
      BranchUrlBuilder branchUrlBuilder) {

    checkIdsStartWithPrefix(
        branchNotificationMessageSendersConfigurationProperties.getNoop().keySet(), "noop");

    Map<String, BranchNotificationMessageSender> mapIdToBranchNotificationMessageSender =
        new HashMap<>();

    mapIdToBranchNotificationMessageSender.putAll(
        createNoopInstances(branchNotificationMessageSendersConfigurationProperties));
    return mapIdToBranchNotificationMessageSender;
  }

  private void checkIdsStartWithPrefix(Set<String> ids, String prefix) {
    String prefixWithDash = prefix + "-";
    if (!ids.stream().allMatch(key -> key.startsWith(prefixWithDash))) {
      throw new RuntimeException("name must start with prefix: " + prefixWithDash);
    }
  }

  private Map<String, BranchNotificationMessageSenderNoop> createNoopInstances(
      BranchNotificationMessageSendersConfigurationProperties
          branchNotificationMessageSendersConfigurationProperties) {
    Map<String, BranchNotificationMessageSenderNoop> noop =
        branchNotificationMessageSendersConfigurationProperties.getNoop().entrySet().stream()
            .map(
                e -> {
                  String id = e.getKey();
                  NoopConfigurationProperties noopConfigurationProperties = e.getValue();
                  if (!noopConfigurationProperties.isEnabled()) {
                    throw new RuntimeException("only enable is accepted value");
                  }
                  BranchNotificationMessageSenderNoop branchNotificationMessageSenderNoop =
                      new BranchNotificationMessageSenderNoop(id);
                  return new SimpleEntry<String, BranchNotificationMessageSenderNoop>(
                      id, branchNotificationMessageSenderNoop);
                })
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    return noop;
  }
}
