export default
class DashboardSearcherParameters {
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

    page(page) {
        this.params.page = page;
    }

    size(size) {
        this.params.size = size;
    }

    getParams(){
        return this.params;
    }

    static isReadyForDashboardSearching(dashboardSearcherParams) {
        return dashboardSearcherParams.params.search || dashboardSearcherParams.params.createdByUserName;
    }
}