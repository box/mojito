import alt from "../../alt";

class WorkbenchActions {

    constructor() {
        this.generateActions(
            "searchParamsChanged",
            "searchResultsReceivedSuccess",
            "searchResultsReceivedError",
            "saveTextUnit",
            "saveTextUnitSuccess",
            "saveTextUnitError",
            "checkAndSaveTextUnit",
            "checkAndSaveTextUnitSuccess",
            "checkAndSaveTextUnitError",
            "deleteTextUnits",
            "deleteTextUnitsSuccess",
            "deleteTextUnitsError",
            "saveVirtualAssetTextUnit",
            "saveVirtualAssetTextUnitSuccess",
            "saveVirtualAssetTextUnitError",
            "textUnitSelection",
            "reviewTextUnits",
            "resetErrorState",
            "resetAllSelectedTextUnits",
            "resetSelectedTextUnitsInCurrentPage",
            "selectAllTextUnitsInCurrentPage"
        );
    }
}

export default alt.createActions(WorkbenchActions);
