package com.box.l10n.mojito.idgenerator;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * An id generator that uses the current text unit content md5 and the previous content md5 to
 * generate near stable ids. Generated ids may be non-unique.
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
 * <p>Instead, {@link ContextAwareMd5IdGenerator} generates an id based on the current source and
 * the previous source. The previous source is used to distinguish multiple occurence of the same
 * source in the asset. This allows to uniquely identify "most" strings in the asset in the same way
 * the sequence based generator does.
 *
 * <p>{@code <p>a</p><p>b</p><p>c</p>} will generate the following pairs {@code ha-hi: a, hb-ha: b,
 * hc-hb: c} and {@code <p>n</p><p>a</p><p>b</p><p>c</p>} will generate the following pairs {@code
 * hn-hi: n, ha-hn: a, hb-ha: b, hc-hb: c}.
 *
 * <p>In addition to the newly created id, one extra id is modified but all the other text unit ids
 * are kept unchanged.
 *
 * <p>Repeated sequence of strings will lead to having non-unique ids generated. For example: {@code
 * <p>a</p><p>b</p><p>a</p><p>b</p>} will generate the following pairs {@code ha-hi: a, hb-ha: b,
 * ha-hb: a, hb-ha: b}. "hb-ha" is a duplicate id.
 *
 * <p>This is a limitation as it prevents having distinct translations depending on the context for
 * those strings. Yet, to get non-unique ids, it would require a very specific asset content.
 *
 * <p>O(1) space complexity
 */
public class ContextAwareMd5IdGenerator {
  String previousMd5;

  public ContextAwareMd5IdGenerator() {
    this.previousMd5 = DigestUtils.md5Hex("");
  }

  public String nextId(String source) {
    final String sourceMd5 = DigestUtils.md5Hex(source);
    String nextId = sourceMd5 + "-" + previousMd5;
    this.previousMd5 = sourceMd5;
    return nextId;
  }
}
