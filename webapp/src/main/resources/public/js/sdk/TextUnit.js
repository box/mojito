import _ from "lodash";

/**
 * Used to communicate with textunits WS (search for translated/untranslated strings, create/update new translation)
 *
 * A TextUnit is bound to a TmTextUnit and a target locale. It represents a current translation or an untranslated string.
 *
 * A TmTextUnit is the entity that represents only a string that needs to be translated, it's not bound to a target locale.
 * Multiple TmTextUnitVariant (translation) are linked to a TmTextUnit.
 *
 */
export default
class TextUnit {

    constructor(data) {
        this.data = data || {};
    }

    /**
     * TextUnit id.
     *
     * When the TextUnit is returned by the TextUnitClient:
     * - If id is not null, it means a current translation exists (and this id is mapping to the TMTextUnitCurrentVariant id).
     * - If id is undefined, it means there is no translation yet (more precisialy, there is no TMTextUnitCurrentVariant for
     * the TMTextUnit and locale).
     *
     * @returns {int}
     */
    getId() {
        return this.data.id;
    }

    setId(id) {
        this.data.id = id;
    }

    /**
     * TmTextUnit id.
     *
     * Must be provided when creating/updating a TextUnit
     *
     * @returns {int}
     */
    getTmTextUnitId() {
        return this.data.tmTextUnitId;
    }

    setTmTextUnitId(tmTextUnitId) {
        this.data.tmTextUnitId = tmTextUnitId;
    }

    getTmTextUnitVariantId() {
        return this.data.tmTextUnitVariantId;
    }

    setTmTextUnitVariantId(tmTextUnitVariantId) {
        this.data.tmTextUnitVariantId = tmTextUnitVariantId;
    }

    getThirdPartyTextUnitId() {
        return this.data.thirdPartyTextUnitId;
    }

    setThirdPartyTextUnitId(thirdPartyTextUnitId) {
        this.data.thirdPartyTextUnitId = thirdPartyTextUnitId;
    }

    /**
     * Locale id.
     *
     * Must be provided when creating/updating a TextUnit
     *
     * @returns {int}
     */
    getLocaleId() {
        return this.data.localeId;
    }


    setLocaleId(localeId) {
        this.data.localeId = localeId;
    }

    getName() {
        return this.data.name;
    }

    setName(name) {
        this.data.name = name;
    }
    
    getAssetPath() {
        return this.data.assetPath;
    }

    setAssetPath(assetPath) {
        this.data.assetPath = assetPath;
    }

    getSource() {
        return this.data.source;
    }

    setSource(source) {
        this.data.source = source;
    }

    getComment() {
        return this.data.comment;
    }

    setComment(comment) {
        this.data.comment = comment;
    }

    /**
     * Target contains the translation
     *
     * Must be provided when creating/updating a TextUnit
     *
     * @returns {*}
     */
    getTarget() {
        return this.data.target;
    }

    setTarget(target) {
        this.data.target = target;
    }

    getTargetLocale() {
        return this.data.targetLocale;
    }

    setTargetLocale(targetLocale) {
        this.data.targetLocale = targetLocale;
    }

    getTargetComment() {
        return this.data.targetComment;
    }

    setTargetComment(targetComment) {
        this.data.targetComment = targetComment;
    }

    getAssetId() {
        return this.data.assetId;
    }

    setAssetId(assetId) {
        this.data.assetId = assetId;
    }

    getLastSuccessfulAssetExtractionId() {
        return this.data.lastSuccessfulAssetExtractionId;
    }

    setLastSuccessfulAssetExtractionId(lastSuccessfulAssetExtractionId) {
        this.data.lastSuccessfulAssetExtractionId = lastSuccessfulAssetExtractionId;
    }

    getAssetExtractionId() {
        return this.data.assetExtractionId;
    }

    setAssetExtractionId(assetExtractionId) {
        this.data.assetExtractionId = assetExtractionId;
    }

    getAssetTextUnitId() {
        return this.data.assetTextUnitId;
    }

    setAssetTextUnitId() {
        this.data.assetTextUnitId = assetTextUnitId;
    }

    getTmTextUnitCurrentVariantId() {
        return this.data.tmTextUnitCurrentVariantId;
    }

    setTmTextUnitCurrentVariantId(tmTextUnitCurrentVariantId) {
        this.data.tmTextUnitCurrentVariantId = tmTextUnitCurrentVariantId;
    }

    getStatus() {
        return this.data.status;
    }

    setStatus(status) {
        this.data.status = status;
    }

    isIncludedInLocalizedFile() {
        return this.data.includedInLocalizedFile;
    }

    setIncludedInLocalizedFile(includedInLocalizedFile) {
        this.data.includedInLocalizedFile = includedInLocalizedFile;
    }

    isUsed() {
        return this.data.used;
    }

    setUsed(used) {
        this.data.used = used;
    }

    isTranslated() {
        return this.data.translated;
    }

    setTranslated(translated) {
        this.data.translated = translated;
    }

    getPluralForm() {
        return this.data.pluralForm;
    }

    setPluralForm(pluralForm) {
        this.data.pluralForm = pluralForm;
    }
    
    getPluralFormOther() {
        return this.data.pluralFormOther;
    }

    setPluralFormOther(pluralFormOther) {
        this.data.pluralFormOther = pluralFormOther;
    }

    getDoNotTranslate() {
        return this.data.doNotTranslate;
    }

    setDoNotTranslate(doNotTranslate) {
        this.data.doNotTranslate = doNotTranslate;
    }

    getRepositoryName() {
        return this.data.repositoryName;
    }

    setRepositoryName(repositoryName) {
        this.data.repositoryName = repositoryName;
    }

    getTmTextUnitCreatedDate() {
        return this.data.tmTextUnitCreatedDate;
    }

    setTmTextUnitCreatedDate(tmTextUnitCreatedDate) {
        this.data.tmTextUnitCreatedDate = tmTextUnitCreatedDate;
    }

    getCreatedDate() {
        return this.data.createdDate;
    }

    setCreatedDate(createdDate) {
        this.data.createdDate = createdDate;
    }

    /**
     * According to the current implementation of the web service, the combination
     * of tmTextUnitId and localeId is a unique key for the TextUnit object. The
     * implementation of this function must be updated if the TextUnit definition is
     * changed in the web service.
     * @returns {string} A string to uniquely identify a TextUnit object
     */
    getTextUnitKey() {
        return this.getTmTextUnitId() + "_" + this.getLocaleId();
    }

    equals(textUnit) {
        return this.getTextUnitKey() === textUnit.getTextUnitKey();
    }

    static toTextUnits(jsonTextUnits) {

        var textUnits = [];

        for (let textUnit of jsonTextUnits) {
            textUnits.push(TextUnit.toTextUnit(textUnit));
        }

        return textUnits;
    }

    static toTextUnit(jsonTextUnit) {

        return new TextUnit(jsonTextUnit);
    }
}

TextUnit.STATUS = {
    "TRANSLATION_NEEDED": "TRANSLATION_NEEDED",
    "REVIEW_NEEDED": "REVIEW_NEEDED",
    "APPROVED": "APPROVED"
};
