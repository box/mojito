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

    deleted(deleted) {
        this.params.deleted = deleted;
    }

    undeleted(undeleted) {
        this.params.undeleted = undeleted;
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
        return dashboardSearcherParams.params.branchId || dashboardSearcherParams.params.createdByUserName;
    }
}