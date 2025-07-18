import alt from "../../alt.js";

class BranchesSearchParamsActions {

    constructor() {
        this.generateActions(
            "changeDeleted",
            "changeUndeleted",
            "changeEmpty",
            "changeNotEmpty",
            "changeOnlyMyBranches",
            "changeSearchText",
            "resetBranchesSearchParams",
            "changeCreatedBefore",
            "changeCreatedAfter",
        );
    }
}

export default alt.createActions(BranchesSearchParamsActions);