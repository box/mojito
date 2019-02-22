import BaseClient from "./BaseClient";

class BranchStatisticClient extends BaseClient {

    getBranches(branchStatisticSearcherParameters) {
        return this.get(this.getUrl(), branchStatisticSearcherParameters.getParams());
    }

    getEntityName() {
        return 'branchStatistics';
    }
};

export default new BranchStatisticClient();