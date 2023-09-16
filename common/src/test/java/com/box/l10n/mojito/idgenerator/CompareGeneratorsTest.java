package com.box.l10n.mojito.idgenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.Test;

/**
 * The tests bellow hilight how the generators would work in the context of string extraction. They
 * don't test the generator per se just compare their benefits and drawbacks.
 */
public class CompareGeneratorsTest {

  /**
   * CountedMd5IdGenerator is best, other generators incure extra id changes (which could be
   * recovered with leveraging in most case).
   */
  @Test
  public void sideEffectOfInsert() {

    /*
     * with {@link CountedMd5IdGenerator}, insert (and delete) don't have any side effect on other
     * text unit ids. Only a new entry is created in the TM. That's the best generator for this use case
     */
    assertThat(Stream.of("a", "b", "a", "b", "c", "d").map(new CountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-0",
            "92eb5ffee6ae2fec3ad71c777531578f-0",
            "0cc175b9c0f1b6a831c399e269772661-1",
            "92eb5ffee6ae2fec3ad71c777531578f-1",
            "4a8a08f09d37b73795649038408b5f33-0",
            "8277e0910d750195b448797616e091ad-0");

    assertThat(
            Stream.of("a", "b", "a", "new", "b", "c", "d").map(new CountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-0",
            "92eb5ffee6ae2fec3ad71c777531578f-0",
            "0cc175b9c0f1b6a831c399e269772661-1",
            "22af645d1859cb5ca6da0c484f1f37ea-0",
            "92eb5ffee6ae2fec3ad71c777531578f-1",
            "4a8a08f09d37b73795649038408b5f33-0",
            "8277e0910d750195b448797616e091ad-0");

    /* {@link ContextAwareMd5IdGenerator} changes 2 ids, but typically it could be leveraged by md5 */
    assertThat(
            Stream.of("a", "b", "a", "b", "c", "d").map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661",
            "0cc175b9c0f1b6a831c399e269772661-92eb5ffee6ae2fec3ad71c777531578f",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f",
            "8277e0910d750195b448797616e091ad-4a8a08f09d37b73795649038408b5f33");

    assertThat(
            Stream.of("a", "b", "a", "new", "b", "c", "d")
                .map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661",
            "0cc175b9c0f1b6a831c399e269772661-92eb5ffee6ae2fec3ad71c777531578f",
            /* change 1 */
            "22af645d1859cb5ca6da0c484f1f37ea-0cc175b9c0f1b6a831c399e269772661",
            /* change 2 */
            "92eb5ffee6ae2fec3ad71c777531578f-22af645d1859cb5ca6da0c484f1f37ea",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f",
            "8277e0910d750195b448797616e091ad-4a8a08f09d37b73795649038408b5f33");

    /* {@link ContextAwareCountedMd5IdGenerator} changes 2 ids, but typically it could be leveraged by md5 */
    assertThat(
            Stream.of("a", "b", "a", "b", "c", "d")
                .map(new ContextAwareCountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e-1",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661-1",
            "0cc175b9c0f1b6a831c399e269772661-92eb5ffee6ae2fec3ad71c777531578f-1",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661-2",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f-2",
            "8277e0910d750195b448797616e091ad-4a8a08f09d37b73795649038408b5f33-1");

    assertThat(
            Stream.of("a", "b", "a", "new", "b", "c", "d")
                .map(new ContextAwareCountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e-1",
            "92eb5ffee6ae2fec3ad71c777531578f-0cc175b9c0f1b6a831c399e269772661-1",
            "0cc175b9c0f1b6a831c399e269772661-92eb5ffee6ae2fec3ad71c777531578f-1",
            /* change 1 */
            "22af645d1859cb5ca6da0c484f1f37ea-0cc175b9c0f1b6a831c399e269772661-2",
            /* change 2 */
            "92eb5ffee6ae2fec3ad71c777531578f-22af645d1859cb5ca6da0c484f1f37ea-1",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f-2",
            "8277e0910d750195b448797616e091ad-4a8a08f09d37b73795649038408b5f33-1");
  }

  /**
   * This covers what I suspect to be the most common case of duplicate source in an asset.
   *
   * <p>It showcases non-stable reference to translations in the TM with CountedMd5IdGenerator.
   * CountedMd5IdGenerator would otherwise be the best generator but this limitation might be
   * problematic for a relatively common use case. That said for an issue to araise, it requires 1)
   * duplicates to have different translations (possible) 2) swapping them in the asset (less
   * likely). Having the sequence of 1 & 2 looks relatively unlikely but ...
   */
  @Test
  public void commonDuplicateInAsset() {
    /* Assume (a, b) and (c, e) delimit obvious section in the asset for the reader (think HTML) */
    assertThat(Stream.of("a", "dup", "b", "c", "dup", "e").map(new CountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-0",
            /* points to translation t1, translated for the "a, b" section */
            "0e9f1e8e40bb79e800b0cc9433830cf4-0",
            "92eb5ffee6ae2fec3ad71c777531578f-0",
            "4a8a08f09d37b73795649038408b5f33-0",
            /* points to translation t2, translated for the "c, e" section */
            "0e9f1e8e40bb79e800b0cc9433830cf4-1",
            "e1671797c52e15f763380b45e841ec32-0");

    /* Moving sections will lead to swap translations of the "dup" text unit. This is problematic only if translations
     * are different. It is pretty obfuscated in the process. Nothing will indicate the translations are wrongly
     * reused in another context.
     *
     * This is the main weakness of CountedMd5IdGenerator. The ids being unique, it will always be possible to fix
     * with post editing and QA but the fact the swap is obfuscated is problematic and that is for what seems
     * to be the "more" common case */
    assertThat(Stream.of("c", "dup", "e", "a", "dup", "b").map(new CountedMd5IdGenerator()::nextId))
        .containsExactly(
            "4a8a08f09d37b73795649038408b5f33-0",
            /* no id change, it will wrongly reuse t1 */
            "0e9f1e8e40bb79e800b0cc9433830cf4-0",
            "e1671797c52e15f763380b45e841ec32-0",
            "0cc175b9c0f1b6a831c399e269772661-0",
            /* no id change, it will wrongly reuse t2 */
            "0e9f1e8e40bb79e800b0cc9433830cf4-1",
            "92eb5ffee6ae2fec3ad71c777531578f-0");

    /* Assume (a, b) and (c, e) delimit obvious section in the asset for the reader (think HTML) */
    assertThat(
            Stream.of("a", "dup", "b", "c", "dup", "e")
                .map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e",
            "0e9f1e8e40bb79e800b0cc9433830cf4-0cc175b9c0f1b6a831c399e269772661",
            "92eb5ffee6ae2fec3ad71c777531578f-0e9f1e8e40bb79e800b0cc9433830cf4",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f",
            "0e9f1e8e40bb79e800b0cc9433830cf4-4a8a08f09d37b73795649038408b5f33",
            "e1671797c52e15f763380b45e841ec32-0e9f1e8e40bb79e800b0cc9433830cf4");

    /* moving section will lead to 2 ids changed, but "dup" ids are kept unchanged, hence proper translation are
     * kept. ContextAwareMd5IdGenerator supports this use case the best, with the least change in ids */
    assertThat(
            Stream.of("c", "dup", "e", "a", "dup", "b")
                .map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            /* id changed for c */
            "4a8a08f09d37b73795649038408b5f33-d41d8cd98f00b204e9800998ecf8427e",
            "0e9f1e8e40bb79e800b0cc9433830cf4-4a8a08f09d37b73795649038408b5f33",
            "e1671797c52e15f763380b45e841ec32-0e9f1e8e40bb79e800b0cc9433830cf4",
            /* id changed for a */
            "0cc175b9c0f1b6a831c399e269772661-e1671797c52e15f763380b45e841ec32",
            "0e9f1e8e40bb79e800b0cc9433830cf4-0cc175b9c0f1b6a831c399e269772661",
            "92eb5ffee6ae2fec3ad71c777531578f-0e9f1e8e40bb79e800b0cc9433830cf4");

    /* Assume (a, b) and (c, e) delimit obvious section in the asset for the reader (think HTML) */
    assertThat(
            Stream.of("a", "dup", "b", "c", "dup", "e")
                .map(new ContextAwareCountedMd5IdGenerator()::nextId))
        .containsExactly(
            "0cc175b9c0f1b6a831c399e269772661-d41d8cd98f00b204e9800998ecf8427e-1",
            "0e9f1e8e40bb79e800b0cc9433830cf4-0cc175b9c0f1b6a831c399e269772661-1",
            "92eb5ffee6ae2fec3ad71c777531578f-0e9f1e8e40bb79e800b0cc9433830cf4-1",
            "4a8a08f09d37b73795649038408b5f33-92eb5ffee6ae2fec3ad71c777531578f-1",
            "0e9f1e8e40bb79e800b0cc9433830cf4-4a8a08f09d37b73795649038408b5f33-1",
            "e1671797c52e15f763380b45e841ec32-0e9f1e8e40bb79e800b0cc9433830cf4-2");

    /* moving section will lead to 4 ids changed! but "dup" ids are kept unchanged, hence proper translation are
     * kept. ContextAwareCountedMd5IdGenerator handles support the use case but is not as good as
     * ContextAwareMd5IdGenerator */
    assertThat(
            Stream.of("c", "dup", "e", "a", "dup", "b")
                .map(new ContextAwareCountedMd5IdGenerator()::nextId))
        .containsExactly(
            /* id changed for c */
            "4a8a08f09d37b73795649038408b5f33-d41d8cd98f00b204e9800998ecf8427e-1",
            "0e9f1e8e40bb79e800b0cc9433830cf4-4a8a08f09d37b73795649038408b5f33-1",
            /* id changed for e */
            "e1671797c52e15f763380b45e841ec32-0e9f1e8e40bb79e800b0cc9433830cf4-1",
            /* id changed for a */
            "0cc175b9c0f1b6a831c399e269772661-e1671797c52e15f763380b45e841ec32-1",
            "0e9f1e8e40bb79e800b0cc9433830cf4-0cc175b9c0f1b6a831c399e269772661-1",
            /* id changed for b */
            "92eb5ffee6ae2fec3ad71c777531578f-0e9f1e8e40bb79e800b0cc9433830cf4-2");
  }

  /** This showcases the weakness of non-unique id generator, but is a bit of an edge case */
  @Test
  public void edgeCaseDuplicateCanCauseNonUnique() {

    /* that's the main weakness of ContextAwareMd5IdGenerator, there are case where "duplicates" are not
    disambiguated, preventing having different translation for them. But it has to be said it would be for
    a shape of asset that is very special.
    */
    assertThat(
            Stream.of("same-ctx", "dup", "same-ctx", "dup")
                .map(new ContextAwareMd5IdGenerator()::nextId))
        .containsExactly(
            /* duplicated id, prevent having different translations (maybe a bit of an edge case though) */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-d41d8cd98f00b204e9800998ecf8427e",
            "0e9f1e8e40bb79e800b0cc9433830cf4-47c3c8a8ef000535e7bb4e5ebc4f6f26",
            /* duplicated id, prevent having different translations (maybe a bit of an edge case though) */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-0e9f1e8e40bb79e800b0cc9433830cf4",
            "0e9f1e8e40bb79e800b0cc9433830cf4-47c3c8a8ef000535e7bb4e5ebc4f6f26");

    /* CountedMd5IdGenerator will never have duplicates */
    assertThat(
            Stream.of("same-ctx", "dup", "same-ctx", "dup")
                .map(new CountedMd5IdGenerator()::nextId))
        .containsExactly(
            /* de-dupped by counter on self */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-0",
            /* de-dupped by counter on self */
            "0e9f1e8e40bb79e800b0cc9433830cf4-0",
            /* de-dupped by counter on self */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-1",
            /* de-dupped by counter on self */
            "0e9f1e8e40bb79e800b0cc9433830cf4-1");

    /* ContextAwareCountedMd5IdGenerator will never have duplicates */
    assertThat(
            Stream.of("same-ctx", "dup", "same-ctx", "dup")
                .map(new ContextAwareCountedMd5IdGenerator()::nextId))
        .containsExactly(
            /* de-dupped by previous */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-d41d8cd98f00b204e9800998ecf8427e-1",
            /* de-dupped by counter on previous */
            "0e9f1e8e40bb79e800b0cc9433830cf4-47c3c8a8ef000535e7bb4e5ebc4f6f26-1",
            /* de-dupped by previous */
            "47c3c8a8ef000535e7bb4e5ebc4f6f26-0e9f1e8e40bb79e800b0cc9433830cf4-1",
            /* de-dupped by counter on previous */
            "0e9f1e8e40bb79e800b0cc9433830cf4-47c3c8a8ef000535e7bb4e5ebc4f6f26-2");
  }
}
