import alt from "../../alt";

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