import alt from "../../alt";

class BranchTextUnitsPaginatorActions {
    constructor() {
        this.generateActions(
            "goToNextPage",
            "goToPreviousPage",
            "changeCurrentPageNumber"
        );
    }
}

export default alt.createActions(BranchTextUnitsPaginatorActions);