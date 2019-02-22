import alt from "../../alt";

class BranchesSearchParamsActions {

    constructor() {
        this.generateActions(
            "changeDeleted",
            "changeUndeleted",
            "changeOnlyMyBranches",
            "changeSearchText",
            "resetBranchesSearchParams",
        );
    }
}

export default alt.createActions(BranchesSearchParamsActions);