package com.box.l10n.mojito.localtm.merger;

import com.box.l10n.mojito.immutables.NoPrefixNoBuiltinContainer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.joda.time.DateTime;

@Value.Immutable
@NoPrefixNoBuiltinContainer
@JsonDeserialize(builder = BranchStateTextUnit.Builder.class)
public abstract class AbstractBranchStateTextUnit {

  @Nullable
  public abstract Long getTmTextUnitId();

  @Nullable
  public abstract Long getAssetTextUnitId();

  @Nullable
  public abstract String getMd5();

  @Nullable
  public abstract DateTime getCreatedDate();

  @Nullable
  public abstract String getName();

  @Nullable
  public abstract String getSource();

  @Nullable
  public abstract String getComments();

  @Nullable
  public abstract String getPluralForm();

  @Nullable
  public abstract String getPluralFormOther();

  @Value.Default
  public ImmutableMap<String, BranchData> getBranchNameToBranchDatas() {
    return ImmutableMap.of();
  }
}
