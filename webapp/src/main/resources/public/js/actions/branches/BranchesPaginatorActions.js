import alt from "../../alt";

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