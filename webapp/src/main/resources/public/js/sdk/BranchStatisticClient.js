import BaseClient from "./BaseClient.js";

class BranchStatisticClient extends BaseClient {

    getBranches(branchStatisticSearcherParameters) {
        return this.get(this.getUrl(), branchStatisticSearcherParameters.getParams());
    }

    getEntityName() {
        return 'branchStatistics';
    }
};

export default new BranchStatisticClient();