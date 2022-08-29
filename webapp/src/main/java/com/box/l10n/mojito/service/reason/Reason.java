package com.box.l10n.mojito.service.reason;

import java.util.ArrayList;
import java.util.List;

/** @author aloison */
public enum Reason {
  COMPILATION_FAILED(ReasonGroup.INTEGRITY_CHECK),
  PLACEHOLDER_MISMATCH(ReasonGroup.INTEGRITY_CHECK),
  INVALID_INPUT_ENCODING(ReasonGroup.INTEGRITY_CHECK);

  private ReasonGroup reasonGroup;

  Reason(ReasonGroup reasonGroup) {
    this.reasonGroup = reasonGroup;
  }

  /**
   * @param group
   * @return Whether the reason is part of the given group
   */
  public boolean isInGroup(ReasonGroup group) {
    return this.reasonGroup == group;
  }

  /**
   * @param group
   * @return All the reasons belonging to the given group
   */
  public static List<Reason> getAllReasonsInGroup(ReasonGroup group) {
    List<Reason> allReasons = new ArrayList<>();

    for (Reason reason : Reason.values()) {
      if (reason.isInGroup(group)) {
        allReasons.add(reason);
      }
    }

    return allReasons;
  }

  /**
   * @param group
   * @return All the reasons belonging to the given group, as strings
   */
  public static List<String> getAllReasonsInGroupAsString(ReasonGroup group) {
    List<String> allReasonsAsString = new ArrayList<>();

    for (Reason reason : getAllReasonsInGroup(group)) {
      allReasonsAsString.add(reason.toString());
    }

    return allReasonsAsString;
  }

  public enum ReasonGroup {
    INTEGRITY_CHECK;
  }
}
