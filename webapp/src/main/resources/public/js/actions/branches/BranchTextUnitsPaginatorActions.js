import alt from "../../alt.js";

class BranchTextUnitsPaginatorActions {
    constructor() {
        this.generateActions(
            "goToNextPage",
            "goToPreviousPage",
            "changePageSize",
        );
    }
}

export default alt.createActions(BranchTextUnitsPaginatorActions);