package com.box.l10n.mojito.android.strings;

import java.util.ArrayList;
import java.util.List;

public class AndroidStringDocument {

  private List<AbstractAndroidString> strings = new ArrayList<>();
  private List<AndroidSingular> singulars = new ArrayList<>();
  private List<AndroidPlural> plurals = new ArrayList<>();

  public List<AndroidSingular> getSingulars() {
    return singulars;
  }

  public List<AndroidPlural> getPlurals() {
    return plurals;
  }

  public List<AbstractAndroidString> getStrings() {
    return strings;
  }

  public void addSingular(AndroidSingular string) {
    singulars.add(string);
    strings.add(string);
  }

  public void addPlural(AndroidPlural plural) {
    plurals.add(plural);
    strings.add(plural);
  }
}
