package com.box.l10n.mojito.idgenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.Test;

public class ContextAwareMd5IdGeneratorTest {

  @Test
  public void nextId() {
    assertThat(
            Stream.of("a", "b", "a", "b", "c", "d").map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661",
            "0cc175b9c0f1b6a831c399e269772661-92eb5ffee6ae2fec3ad71c777531578f",
            // notice the duplicate id for the 2 "b" values - read class doc for related
            // limitation
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f",
            "8277e0910d750195b448797616e091ad-4a8a08f09d37b73795649038408b5f33");
  }
}
