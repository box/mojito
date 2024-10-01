import BaseClient from "./BaseClient";

class BranchTextUnitClient extends BaseClient {
    getBranchTextUnits(branchStatisticId, parameters) {
        return this.get(
            `${this.getUrl(branchStatisticId)}/branchTextUnitStatistics`,
            parameters.getParams()
        );
    }

    getEntityName() {
        return 'branchStatistics';
    }
}

export default new BranchTextUnitClient();
