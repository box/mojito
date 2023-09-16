package com.box.l10n.mojito.idgenerator;

import com.google.common.collect.HashMultiset;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * An id generator that uses the current text unit content md5 and an occurence counter to generate
 * near stable ids.
 *
 * <p>Generated ids are unique but the use of the counter to distinguish duplicated source leads to
 * non-deterministic references to text units and their translations saved in the translation
 * memory.
 *
 * <p>The goal of this generator is to generate near stable ids that points to the same source
 * content in between asset revisions. Specifically, when the asset structure only is modified or
 * small source edits are made, it avoids generating a brand-new set of text units that will require
 * full re-processing.
 *
 * <p>For example the default HTML {@link net.sf.okapi.common.IdGenerator} uses sequences. If using
 * those ids as text unit names then any change to the HTML structure leads to all the text unit
 * names to point to different source content between different revisions.
 *
 * <p>{@code <p>a</p><p>b</p>} will generate the following pairs {@code tu1: a, tu2: b} and {@code
 * <p>n</p><p>a</p><p>b</p>} will generate the following pairs {@code tu1: n, tu2: a, tu3: b}
 *
 * <p>Instead, {@link CountedMd5IdGenerator} generates an id based on the current source and the
 * number of occurence of the current source. The number of occurrence is used to distinguish
 * multiple occurence of the same source. This allows to identify every single string in the asset
 * uniquely in the same way the sequence based generator does.
 *
 * <p>{@code <p>a</p><p>b</p><p>c</p>} will generate the following pairs {@code ha-1: a, hb-1: b,
 * hc-1: c} and {@code <p>n</p><p>a</p><p>b</p><p>c</p>} will generate the following pairs {@code
 * hn-1: n, ha-1: a, hb-1: b, hc-1: c}.
 *
 * <p>{@code <p></p><p>a</p><p>b</p><p>c</p>}
 *
 * <p>Complexity is O(N) space.
 */
public class CountedMd5IdGenerator {

  HashMultiset<String> md5s;

  public CountedMd5IdGenerator() {
    this.md5s = HashMultiset.create();
  }

  public String nextId(String source) {
    String sourceMd5 = DigestUtils.md5Hex(source);
    String nextId = sourceMd5 + "-" + md5s.count(sourceMd5);
    this.md5s.add(sourceMd5);
    return nextId;
  }
}
