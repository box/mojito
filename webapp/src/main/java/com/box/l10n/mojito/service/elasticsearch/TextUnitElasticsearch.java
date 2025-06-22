package com.box.l10n.mojito.service.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.StringJoiner;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Document(indexName = "text-units")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TextUnitElasticsearch {

  @Id Long id;

  @Field(type = FieldType.Keyword)
  String name;

  @Field(type = FieldType.Keyword)
  String repositoryName;

  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {@InnerField(suffix = "autocomplete", type = FieldType.Search_As_You_Type)})
  String source;

  @Field(type = FieldType.Keyword)
  String targetLocale;

  @Field(type = FieldType.Text, analyzer = "arabic")
  String targetAr;

  @Field(type = FieldType.Text, analyzer = "armenian")
  String targetHy;

  @Field(type = FieldType.Text, analyzer = "basque")
  String targetEu;

  @Field(type = FieldType.Text, analyzer = "bengali")
  String targetBn;

  @Field(type = FieldType.Text, analyzer = "brazilian")
  String targetPtBr;

  @Field(type = FieldType.Text, analyzer = "bulgarian")
  String targetBg;

  @Field(type = FieldType.Text, analyzer = "catalan")
  String targetCa;

  @Field(type = FieldType.Text, analyzer = "cjk")
  String targetCjk;

  @Field(type = FieldType.Text, analyzer = "czech")
  String targetCs;

  @Field(type = FieldType.Text, analyzer = "danish")
  String targetDa;

  @Field(type = FieldType.Text, analyzer = "dutch")
  String targetNl;

  @Field(type = FieldType.Text, analyzer = "english")
  String targetEn;

  @Field(type = FieldType.Text, analyzer = "estonian")
  String targetEt;

  @Field(type = FieldType.Text, analyzer = "finnish")
  String targetFi;

  @Field(type = FieldType.Text, analyzer = "french")
  String targetFr;

  @Field(type = FieldType.Text, analyzer = "galician")
  String targetGl;

  @Field(type = FieldType.Text, analyzer = "german")
  String targetDe;

  @Field(type = FieldType.Text, analyzer = "greek")
  String targetEl;

  @Field(type = FieldType.Text, analyzer = "hindi")
  String targetHi;

  @Field(type = FieldType.Text, analyzer = "hungarian")
  String targetHu;

  @Field(type = FieldType.Text, analyzer = "indonesian")
  String targetId;

  @Field(type = FieldType.Text, analyzer = "irish")
  String targetGa;

  @Field(type = FieldType.Text, analyzer = "italian")
  String targetIt;

  @Field(type = FieldType.Text, analyzer = "latvian")
  String targetLv;

  @Field(type = FieldType.Text, analyzer = "lithuanian")
  String targetLt;

  @Field(type = FieldType.Text, analyzer = "norwegian")
  String targetNo;

  @Field(type = FieldType.Text, analyzer = "persian")
  String targetFa;

  @Field(type = FieldType.Text, analyzer = "portuguese")
  String targetPt;

  @Field(type = FieldType.Text, analyzer = "romanian")
  String targetRo;

  @Field(type = FieldType.Text, analyzer = "russian")
  String targetRu;

  @Field(type = FieldType.Text, analyzer = "serbian")
  String targetSr;

  @Field(type = FieldType.Text, analyzer = "sorani")
  String targetCkb;

  @Field(type = FieldType.Text, analyzer = "spanish")
  String targetEs;

  @Field(type = FieldType.Text, analyzer = "swedish")
  String targetSv;

  @Field(type = FieldType.Text, analyzer = "turkish")
  String targetTr;

  @Field(type = FieldType.Text, analyzer = "thai")
  String targetTh;

  /**
   * Eventually setup "kuromoji", but needs dedicated configurations @Setting(settingPath =
   * "/your-index-settings.json")
   *
   * <p>{ "analysis": { "analyzer": { "kuromoji_analyzer": { "type": "custom", "tokenizer":
   * "kuromoji_tokenizer" } } } }
   */
  @Field(type = FieldType.Text, analyzer = "cjk")
  String targetJa;

  /** Eventually "nori" */
  @Field(type = FieldType.Text, analyzer = "cjk")
  String targetKo;

  /** Eventually "smartcn" */
  @Field(type = FieldType.Text, analyzer = "cjk")
  String targetZhHans;

  @Field(type = FieldType.Text, analyzer = "cjk")
  String targetZhHant;

  @Field(type = FieldType.Text, analyzer = "standard")
  String targetOther;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTargetLocale() {
    return targetLocale;
  }

  public void setTargetLocale(String targetLocale) {
    this.targetLocale = targetLocale;
  }

  public String getTargetAr() {
    return targetAr;
  }

  public void setTargetAr(String targetAr) {
    this.targetAr = targetAr;
  }

  public String getTargetHy() {
    return targetHy;
  }

  public void setTargetHy(String targetHy) {
    this.targetHy = targetHy;
  }

  public String getTargetEu() {
    return targetEu;
  }

  public void setTargetEu(String targetEu) {
    this.targetEu = targetEu;
  }

  public String getTargetBn() {
    return targetBn;
  }

  public void setTargetBn(String targetBn) {
    this.targetBn = targetBn;
  }

  public String getTargetPtBr() {
    return targetPtBr;
  }

  public void setTargetPtBr(String targetPtBr) {
    this.targetPtBr = targetPtBr;
  }

  public String getTargetBg() {
    return targetBg;
  }

  public void setTargetBg(String targetBg) {
    this.targetBg = targetBg;
  }

  public String getTargetCa() {
    return targetCa;
  }

  public void setTargetCa(String targetCa) {
    this.targetCa = targetCa;
  }

  public String getTargetCjk() {
    return targetCjk;
  }

  public void setTargetCjk(String targetCjk) {
    this.targetCjk = targetCjk;
  }

  public String getTargetCs() {
    return targetCs;
  }

  public void setTargetCs(String targetCs) {
    this.targetCs = targetCs;
  }

  public String getTargetDa() {
    return targetDa;
  }

  public void setTargetDa(String targetDa) {
    this.targetDa = targetDa;
  }

  public String getTargetNl() {
    return targetNl;
  }

  public void setTargetNl(String targetNl) {
    this.targetNl = targetNl;
  }

  public String getTargetEn() {
    return targetEn;
  }

  public void setTargetEn(String targetEn) {
    this.targetEn = targetEn;
  }

  public String getTargetEt() {
    return targetEt;
  }

  public void setTargetEt(String targetEt) {
    this.targetEt = targetEt;
  }

  public String getTargetFi() {
    return targetFi;
  }

  public void setTargetFi(String targetFi) {
    this.targetFi = targetFi;
  }

  public String getTargetFr() {
    return targetFr;
  }

  public void setTargetFr(String targetFr) {
    this.targetFr = targetFr;
  }

  public String getTargetGl() {
    return targetGl;
  }

  public void setTargetGl(String targetGl) {
    this.targetGl = targetGl;
  }

  public String getTargetDe() {
    return targetDe;
  }

  public void setTargetDe(String targetDe) {
    this.targetDe = targetDe;
  }

  public String getTargetEl() {
    return targetEl;
  }

  public void setTargetEl(String targetEl) {
    this.targetEl = targetEl;
  }

  public String getTargetHi() {
    return targetHi;
  }

  public void setTargetHi(String targetHi) {
    this.targetHi = targetHi;
  }

  public String getTargetHu() {
    return targetHu;
  }

  public void setTargetHu(String targetHu) {
    this.targetHu = targetHu;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  public String getTargetGa() {
    return targetGa;
  }

  public void setTargetGa(String targetGa) {
    this.targetGa = targetGa;
  }

  public String getTargetIt() {
    return targetIt;
  }

  public void setTargetIt(String targetIt) {
    this.targetIt = targetIt;
  }

  public String getTargetLv() {
    return targetLv;
  }

  public void setTargetLv(String targetLv) {
    this.targetLv = targetLv;
  }

  public String getTargetLt() {
    return targetLt;
  }

  public void setTargetLt(String targetLt) {
    this.targetLt = targetLt;
  }

  public String getTargetNo() {
    return targetNo;
  }

  public void setTargetNo(String targetNo) {
    this.targetNo = targetNo;
  }

  public String getTargetFa() {
    return targetFa;
  }

  public void setTargetFa(String targetFa) {
    this.targetFa = targetFa;
  }

  public String getTargetPt() {
    return targetPt;
  }

  public void setTargetPt(String targetPt) {
    this.targetPt = targetPt;
  }

  public String getTargetRo() {
    return targetRo;
  }

  public void setTargetRo(String targetRo) {
    this.targetRo = targetRo;
  }

  public String getTargetRu() {
    return targetRu;
  }

  public void setTargetRu(String targetRu) {
    this.targetRu = targetRu;
  }

  public String getTargetSr() {
    return targetSr;
  }

  public void setTargetSr(String targetSr) {
    this.targetSr = targetSr;
  }

  public String getTargetCkb() {
    return targetCkb;
  }

  public void setTargetCkb(String targetCkb) {
    this.targetCkb = targetCkb;
  }

  public String getTargetEs() {
    return targetEs;
  }

  public void setTargetEs(String targetEs) {
    this.targetEs = targetEs;
  }

  public String getTargetSv() {
    return targetSv;
  }

  public void setTargetSv(String targetSv) {
    this.targetSv = targetSv;
  }

  public String getTargetTr() {
    return targetTr;
  }

  public void setTargetTr(String targetTr) {
    this.targetTr = targetTr;
  }

  public String getTargetTh() {
    return targetTh;
  }

  public void setTargetTh(String targetTh) {
    this.targetTh = targetTh;
  }

  public String getTargetJa() {
    return targetJa;
  }

  public void setTargetJa(String targetJa) {
    this.targetJa = targetJa;
  }

  public String getTargetKo() {
    return targetKo;
  }

  public void setTargetKo(String targetKo) {
    this.targetKo = targetKo;
  }

  public String getTargetZhHans() {
    return targetZhHans;
  }

  public void setTargetZhHans(String targetZhHans) {
    this.targetZhHans = targetZhHans;
  }

  public String getTargetZhHant() {
    return targetZhHant;
  }

  public void setTargetZhHant(String targetZhHant) {
    this.targetZhHant = targetZhHant;
  }

  public String getTargetOther() {
    return targetOther;
  }

  public void setTargetOther(String targetOther) {
    this.targetOther = targetOther;
  }

  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner(", ", "TextUnitElasticsearch{", "}");

    if (id != null) sj.add("id=" + id);
    if (name != null) sj.add("name='" + name + '\'');
    if (source != null) sj.add("source='" + source + '\'');
    if (targetLocale != null) sj.add("targetLocale='" + targetLocale + '\'');

    if (targetAr != null) sj.add("targetAr='" + targetAr + '\'');
    if (targetHy != null) sj.add("targetHy='" + targetHy + '\'');
    if (targetEu != null) sj.add("targetEu='" + targetEu + '\'');
    if (targetBn != null) sj.add("targetBn='" + targetBn + '\'');
    if (targetPtBr != null) sj.add("targetPtBr='" + targetPtBr + '\'');
    if (targetBg != null) sj.add("targetBg='" + targetBg + '\'');
    if (targetCa != null) sj.add("targetCa='" + targetCa + '\'');
    if (targetCjk != null) sj.add("targetCjk='" + targetCjk + '\'');
    if (targetCs != null) sj.add("targetCs='" + targetCs + '\'');
    if (targetDa != null) sj.add("targetDa='" + targetDa + '\'');
    if (targetNl != null) sj.add("targetNl='" + targetNl + '\'');
    if (targetEn != null) sj.add("targetEn='" + targetEn + '\'');
    if (targetEt != null) sj.add("targetEt='" + targetEt + '\'');
    if (targetFi != null) sj.add("targetFi='" + targetFi + '\'');
    if (targetFr != null) sj.add("targetFr='" + targetFr + '\'');
    if (targetGl != null) sj.add("targetGl='" + targetGl + '\'');
    if (targetDe != null) sj.add("targetDe='" + targetDe + '\'');
    if (targetEl != null) sj.add("targetEl='" + targetEl + '\'');
    if (targetHi != null) sj.add("targetHi='" + targetHi + '\'');
    if (targetHu != null) sj.add("targetHu='" + targetHu + '\'');
    if (targetId != null) sj.add("targetId='" + targetId + '\'');
    if (targetGa != null) sj.add("targetGa='" + targetGa + '\'');
    if (targetIt != null) sj.add("targetIt='" + targetIt + '\'');
    if (targetLv != null) sj.add("targetLv='" + targetLv + '\'');
    if (targetLt != null) sj.add("targetLt='" + targetLt + '\'');
    if (targetNo != null) sj.add("targetNo='" + targetNo + '\'');
    if (targetFa != null) sj.add("targetFa='" + targetFa + '\'');
    if (targetPt != null) sj.add("targetPt='" + targetPt + '\'');
    if (targetRo != null) sj.add("targetRo='" + targetRo + '\'');
    if (targetRu != null) sj.add("targetRu='" + targetRu + '\'');
    if (targetSr != null) sj.add("targetSr='" + targetSr + '\'');
    if (targetCkb != null) sj.add("targetCkb='" + targetCkb + '\'');
    if (targetEs != null) sj.add("targetEs='" + targetEs + '\'');
    if (targetSv != null) sj.add("targetSv='" + targetSv + '\'');
    if (targetTr != null) sj.add("targetTr='" + targetTr + '\'');
    if (targetTh != null) sj.add("targetTh='" + targetTh + '\'');
    if (targetJa != null) sj.add("targetJa='" + targetJa + '\'');
    if (targetKo != null) sj.add("targetKo='" + targetKo + '\'');
    if (targetZhHans != null) sj.add("targetZhHans='" + targetZhHans + '\'');
    if (targetZhHant != null) sj.add("targetZhHant='" + targetZhHant + '\'');
    if (targetOther != null) sj.add("targetOther='" + targetOther + '\'');

    return sj.toString();
  }
}
