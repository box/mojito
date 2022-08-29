package com.box.l10n.mojito.android.strings;

import java.util.StringJoiner;

public class AndroidPluralItem {

  private final Long id;
  private final AndroidPluralQuantity quantity;
  private final String content;

  public AndroidPluralItem(Long id, AndroidPluralQuantity quantity, String content) {
    this.quantity = quantity;
    this.id = id;
    this.content = content;
  }

  public AndroidPluralItem(String quantity, Long id, String content) {
    this(id, AndroidPluralQuantity.valueOf(quantity.toUpperCase()), content);
  }

  public Long getId() {
    return id;
  }

  public AndroidPluralQuantity getQuantity() {
    return quantity;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", AndroidPluralItem.class.getSimpleName() + "[", "]")
        .add("id=" + id)
        .add("quantity=" + quantity)
        .add("content='" + content + "'")
        .toString();
  }
}
