export default class BranchStatisticSearcherParameters {
    constructor() {
        this.params = {};
    }

    createdByUserName(createdByUserName) {
        this.params.createdByUserName = createdByUserName;
        return this;
    }

    branchId(branchId) {
        this.params.branchId = branchId;
        return this;
    }

    branchName(branchName) {
        this.params.branchName = branchName;
        return this;
    }

    search(search) {
        this.params.search = search;
        return this;
    }

    deleted(deleted) {
        this.params.deleted = deleted;
    }

    createdBefore(createdBefore) {
        this.params.createdBefore = createdBefore
    }

    createdAfter(createdAfter) {
        this.params.createdAfter = createdAfter
    }

    empty(empty) {
        this.params.empty = empty;
    }

    page(page) {
        this.params.page = page;
    }

    size(size) {
        this.params.size = size;
    }

    getParams() {
        return this.params;
    }
}
