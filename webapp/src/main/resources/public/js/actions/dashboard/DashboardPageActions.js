import alt from "../../alt";

class DashboardPageActions {

    constructor() {
        this.generateActions(
            "updateSearchParams",
            "getBranches",
            "getBranchesSuccess",
            "getBranchesError",
            "uploadScreenshotImage",
            "uploadScreenshotImageSuccess",
            "uploadScreenshotImageError",
            "uploadScreenshot",
            "uploadScreenshotSuccess",
            "uploadScreenshotError",
            "textUnitCheckboxChanged",
            "onBranchCollapseChange",
            "onScreenshotUploadModalOpen",
            "onScreenshotUploadModalClose",
            "onImageChoose",
            "selectAllTextUnitsInCurrentPage",
            "resetAllSelectedTextUnitsInCurrentPage",
            "fetchPreviousPage",
            "fetchNextPage"

        );
    }
}

export default alt.createActions(DashboardPageActions);