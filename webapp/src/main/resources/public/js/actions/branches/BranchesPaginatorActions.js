import alt from "../../alt.js";

class BranchesPaginatorActions {

    constructor() {
        this.generateActions(
            "goToNextPage",
            "goToPreviousPage",
            "changeCurrentPageNumber"
        );
    }
}

export default alt.createActions(BranchesPaginatorActions);