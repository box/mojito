package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Restclient {@code IntegrityCheckerType} is a name-only mirror of this server enum. Jackson
 * deserializes by name, not ordinal — ordinals already diverge ({@code SIMPLE_PRINTF_LIKE}).
 */
public class IntegrityCheckerTypeTest {

  @Test
  public void restclientEnumHasTheSameNameSetAsTheServer() {
    Set<String> serverNames =
        Arrays.stream(IntegrityCheckerType.values()).map(Enum::name).collect(Collectors.toSet());
    Set<String> restclientNames =
        Arrays.stream(com.box.l10n.mojito.rest.entity.IntegrityCheckerType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    Set<String> onlyOnServer = new TreeSet<>(serverNames);
    onlyOnServer.removeAll(restclientNames);
    Set<String> onlyOnRestclient = new TreeSet<>(restclientNames);
    onlyOnRestclient.removeAll(serverNames);

    assertEquals(Set.of(), onlyOnServer, "on server but missing from restclient");
    assertEquals(Set.of(), onlyOnRestclient, "on restclient but missing from server");
  }
}
