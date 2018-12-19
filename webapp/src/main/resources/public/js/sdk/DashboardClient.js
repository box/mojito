import BaseClient from "./BaseClient";

class DashboardClient extends BaseClient {

    getBranches(dashboardSearcherParameters) {
        //return this.get(this.getUrl(), dashboardSearcherParameters.getParams());
        return this.get(this.getUrl(), {
            branchId: 3,
            deleted: false,
            page: 0,
            size: 10
        });
    }


    getEntityName() {
        return 'branchStatistics';
    }
};

export default new DashboardClient();