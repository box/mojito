package com.box.l10n.mojito.android.strings;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AndroidPluralTest {

    @Test
    public void testPluralBuilder(){
        AndroidPlural plural;
        List<AndroidPluralItem> items;

        items = ImmutableList.of(
                new AndroidPluralItem("one", 100L, "content one"),
                new AndroidPluralItem("other", 101L, "content other"),
                new AndroidPluralItem("many", 102L, "content many"));

        plural = new AndroidPlural("plural", "comment", items);

        assertThat(plural).isNotNull();
        assertThat(plural.getComment()).isEqualTo("comment");
        assertThat(plural.getName()).isEqualTo("plural");
        assertThat(plural.getItems().get(AndroidPluralQuantity.ONE).getId()).isEqualTo(100L);
        assertThat(plural.getItems().get(AndroidPluralQuantity.ONE).getContent()).isEqualTo("content one");
        assertThat(plural.getItems().get(AndroidPluralQuantity.OTHER).getId()).isEqualTo(101L);
        assertThat(plural.getItems().get(AndroidPluralQuantity.OTHER).getContent()).isEqualTo("content other");
        assertThat(plural.getItems().get(AndroidPluralQuantity.MANY).getId()).isEqualTo(102L);
        assertThat(plural.getItems().get(AndroidPluralQuantity.MANY).getContent()).isEqualTo("content many");

        List<AndroidPluralItem> badItems = ImmutableList.of(
                new AndroidPluralItem("one", 100L, "content one"),
                new AndroidPluralItem("one", 101L, "content one")
        );

        assertThatThrownBy(() -> new AndroidPlural("plural", "comment", badItems))
                .hasMessageContaining("A duplicate was found when building an Android Plural")
                .isInstanceOf(AndroidPluralDuplicateKeyException.class);
    }

}
