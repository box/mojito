import BaseClient from "./BaseClient";

class DashboardClient extends BaseClient {

    getBranches(dashboardSearcherParameters) {
        return this.get(this.getUrl(), dashboardSearcherParameters.getParams());
    }

    getEntityName() {
        return 'branchStatistics';
    }
};

export default new DashboardClient();