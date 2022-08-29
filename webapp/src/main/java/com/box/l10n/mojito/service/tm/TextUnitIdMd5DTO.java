package com.box.l10n.mojito.service.tm;

/** @author wyau */
public class TextUnitIdMd5DTO {
  private Long id;
  private String md5;

  public TextUnitIdMd5DTO(Long id, String md5) {
    this.id = id;
    this.md5 = md5;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }
}
