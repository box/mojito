import alt from "../../alt";

class DashboardHistoryActions {

    constructor() {
        this.generateActions( 
            "enableHistoryUpdate",
            "disableHistoryUpdate",
        );
    }
}

export default alt.createActions(DashboardHistoryActions);
