/**
 * Defines the search parameters for the TextUnitClient.
 *
 * It uses a filter logic. The dataset will be filtered down as more criteria are defined in this object.
 */
export default
class TextUnitSearcherParameters {

    constructor() {
        this.params = {};

        this.ALL = 'ALL';
        this.TRANSLATED = 'TRANSLATED';
        this.NEEDS_TRANSLATION = 'NEEDS_TRANSLATION';
        this.NEEDS_REVIEW = 'NEEDS_REVIEW';
        this.REJECTED = 'REJECTED';

        this.USED = 'USED';
        this.UNUSED = 'UNUSED';
    }

    /**
     * @param repositoryIds - Array of repository IDs that must be sent as part of the search criteria
     * @returns {TextUnitSearcherParameters}
     */
    repositoryIds(repositoryIds) {
        this.params.repositoryIds = repositoryIds;
        return this;
    }

    tmTextUnitIds(tmTextUnitIds) {
        this.params.tmTextUnitIds = tmTextUnitIds;
    }

    locationUsage(locationUsage) {
        this.params.locationUsage = locationUsage;
    }

    branchId(branchId) {
        this.params.branchId = branchId;
    }

    localeTags(localeTags) {
        this.params.localeTags = localeTags;
        return this;
    }

    name(name) {
        this.params.name = name;
        return this;
    }

    source(source) {
        this.params.source = source;
        return this;
    }

    target(target) {
        this.params.target = target;
        return this;
    }

    assetPath(assetPath) {
        this.params.assetPath = assetPath;
        return this;
    }

    pluralFormOther(pluralFormOther) {
        this.params.pluralFormOther = pluralFormOther;
        return this;
    }

    searchType(searchType) {
        this.params.searchType = searchType;
        return this;
    }

    usedFilter(usedFilter) {
        this.params.usedFilter = usedFilter;
        return this;
    }

    doNotTranslateFilter(doNotTranslateFilter) {
        this.params.doNotTranslateFilter = doNotTranslateFilter;
        return this;
    }

    tmTextUnitCreatedBefore(tmTextUnitCreatedBefore) {
        this.params.tmTextUnitCreatedBefore = tmTextUnitCreatedBefore;
        return this;
    }

    tmTextUnitCreatedAfter(tmTextUnitCreatedAfter) {
        this.params.tmTextUnitCreatedAfter = tmTextUnitCreatedAfter;
        return this;
    }

    statusFilter(statusFilter) {
        this.params.statusFilter = statusFilter;
        return this;
    }

    limit(limit) {
        this.params.limit = limit;
        return this;
    }

    offset(offset) {
        this.params.offset = offset;
        return this;
    }

    getParams() {
        return this.params;
    }
}
;

