
class PaginatorStore {

    constructor(actions) {
        this.setDefaultState();
    }
    
    setDefaultState() {
        this.currentPageNumber = 1;
        this.hasNextPage = true;
        this.disabled = true;
        this.shown = true;
        this.limit = 10;
    }

    resetSearchParams() {
        this.setDefaultState();
    }
    
    goToNextPage() {
        if (this.hasNextPage) {
            this.currentPageNumber++;
        } else {
            console.error("There is no next page, goToNextPage shouldn't be called");
        }
    }

    goToPreviousPage() {
        if (this.currentPageNumber > 1) {
            this.currentPageNumber--;
        } else {
            console.error("There is no previous page, goToPreviousPage shouldn't be called");
        }
    }

    changeCurrentPageNumber(currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    performSearch() {
        this.disabled = true;
    }

    searchResultsReceivedSuccess(result) {
        this.disabled = false;
        this.hasNextPage = result.hasNext;
        this.shown = result.size > 0;
    }

    searchResultsReceivedError() {
        this.disabled = false;
        this.shown = false;
    }
   
}

export default PaginatorStore;
