import alt from "../../alt";
import DashboardDataSource from "../../actions/dashboard/DashboardDataSource";
import DashboardSearchParamStore from "./DashboardSearchParamStore";
import DashboardPageActions from "../../actions/dashboard/DashboardPageActions";

class DashboardStore {
    constructor() {
        /**
         *
         * @type {branchStatistics[]}
         */
        this.setDefaultState();

        this.bindActions(DashboardPageActions);
        this.registerAsync(DashboardDataSource);

    }

    setDefaultState() {
        this.hasNext= false;
        this.hasPrevious= false;
        this.size = 10;
        this.currentPageNumber = 1;
        this.first = true;
        this.numberOfElements = 0;
        this.totalPages = 1;
        this.totalElements = 0;
        this.last = true;
        this.branchStatistics = [];
        this.searching = false;
        this.showScreenshotUploadModal = false;
        this.image = null;
        this.screenshotUploaded = [];
        this.textUnitChecked = [];
        this.isBranchOpen = [];
        this.openBranchIndex = -1;
        this.numberOfTextUnitChecked = 0;
        this.totalTextUnitsInPage = 0;
    }

    getBranches() {
        this.waitFor(DashboardSearchParamStore);
        this.getInstance().performDashboardSearch();
        this.isSearching = true;

    }

    getBranchesSuccess(branchStatistics) {
        this.hasNext = branchStatistics.hasNext;
        this.hasPrevious = branchStatistics.hasPrevious;
        this.currentPageNumber = branchStatistics.number;
        this.first = branchStatistics.first;
        this.numberOfElements = branchStatistics.numberOfElements;
        this.totalPages = branchStatistics.totalPages;
        this.totalElements = branchStatistics.totalElements;
        this.last = branchStatistics.last;
        this.branchStatistics = branchStatistics.content;
        this.isSearching = false;
        this.isBranchOpen = Array.apply(null, Array(branchStatistics.length)).map(function () {
            return false;
        });
        this.totalTextUnitsInPage = 0;
        for (let i = 0; i < this.branchStatistics.length; i++) {
            //TODO: update screenshot data
            this.textUnitChecked.push(Array.apply(null, Array(this.branchStatistics[i].branchTextUnitStatistics.length)).map(function () {
                return false
            }));
            this.totalTextUnitsInPage += this.branchStatistics[i].branchTextUnitStatistics.length;
        }


    }

    onScreenshotUploadModalOpen() {
        this.showScreenshotUploadModal = true;
    }

    onScreenshotUploadModalClose() {
        this.showScreenshotUploadModal = false;
    }

    onBranchCollapseChange(index) {
        this.resetAllSelectedTextUnitsInCurrentPage();
        let isOpen =  !this.isBranchOpen[index];
        this.isBranchOpen.fill(false);
        this.isBranchOpen[index] = isOpen;
        if(isOpen) {
            this.openBranchIndex = index;
        }
    }

    textUnitCheckboxChanged(index) {
        this.textUnitChecked[index.index0][index.index1] = !this.textUnitChecked[index.index0][index.index1];
        this.numberOfTextUnitChecked +=  this.textUnitChecked[index.index0][index.index1] ? 1 : -1;
    }

    onImageChoose(image) {
        this.image = image;
    }

    uploadScreenshotImage() {
        this.getInstance().performUploadScreenshotImage();
    }

    uploadScreenshotImageSuccess() {
        // TODO: set imageUrl to this.images[this.uploadingIndex]
        this.getInstance().performUploadScreenshot();
        this.image.url = '/api/images/testing'
    }

    uploadScreenshotImageError() {
        // TODO: show upload failure
        this.resetAllSelectedTextUnitsInCurrentPage();
    }

    uploadScreenshotSuccess() {
        this.getInstance().performDashboardSearch();
    }

    uploadScreenshotError() {
        // TODO: show upload failure
        this.resetAllSelectedTextUnitsInCurrentPage();
    }

    selectAllTextUnitsInCurrentPage() {
        this.textUnitChecked.forEach(e => e.fill(true));
        this.numberOfTextUnitChecked = this.totalTextUnitsInPage;
    }

    resetAllSelectedTextUnitsInCurrentPage() {
        this.textUnitChecked.forEach(e => e.fill(false));
        this.numberOfTextUnitChecked = 0;
    }

    fetchPreviousPage() {
        if(this.currentPageNumber > 0) {
            currentPageNumber--;
            this.getBranches();
        }
    }

    fetchNextPage() {
        if(this.currentPageNumber < this.totalPages) {
            currentPageNumber++;
            this.getBranches();
        }
    }
}

export default alt.createStore(DashboardStore, "DashboardStore")