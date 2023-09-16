package com.box.l10n.mojito.idgenerator;

import com.google.common.collect.HashMultiset;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * An id generator that uses the current text unit content md5, the previous content md5 and an
 * occurence counter to generate near stable ids.
 *
 * <p>Generated ids are unique. The context awareness mitigates an issue seen in {@link
 * CountedMd5IdGenerator} when swapping duplicated string, but it is at the cost of having extra
 * changes of ids similar to {@link ContextAwareMd5IdGenerator}, but even more in some cases.
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
 * <p>Instead, {@link ContextAwareCountedMd5IdGenerator} generates an id based on the current
 * source, the previous source and the number of occurence of the previous source. The number of
 * occurrence is used to distinguish multiple occurence of the same source. This allows to identify
 * every single string in the asset uniquely in the same way the sequence based generator does.
 *
 * <p>A drawback is that if a duplicated source is added at the top of the asset it will propagate
 * to other ids in a similar way as with the sequence based generator. However, the number of
 * changes will be restricted to the count of duplicates present.
 *
 * <p>{@code <p>a</p><p>b</p><p>c</p>} will generate the following pairs {@code ha-hi-1: a, hb-ha-1:
 * b, hc-hb-1: c} and {@code <p>n</p><p>a</p><p>b</p><p>c</p>} will generate the following pairs
 * {@code hn-hi-1: n, ha-hn-1: a, hb-ha-1: b, hc-hb-1: c}.
 *
 * <p>In this example only 2 ids will change when the structure of asset is modified to insert a
 * string at the begining. All other text unit ids keep pointing to the same source.
 *
 * <p>O(N) space complexity
 */
public class ContextAwareCountedMd5IdGenerator {

  HashMultiset<String> previousMd5s;
  String previousMd5;

  public ContextAwareCountedMd5IdGenerator() {
    this.previousMd5 = DigestUtils.md5Hex("");
    this.previousMd5s = HashMultiset.create();
    this.previousMd5s.add(previousMd5);
  }

  public String nextId(String source) {
    final String sourceMd5 = DigestUtils.md5Hex(source);
    String nextId = sourceMd5 + "-" + previousMd5 + "-" + previousMd5s.count(previousMd5);
    this.previousMd5s.add(sourceMd5);
    this.previousMd5 = sourceMd5;
    return nextId;
  }
}
