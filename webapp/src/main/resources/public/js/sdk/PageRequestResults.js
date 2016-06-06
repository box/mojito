export default class PageRequestResults {
    constructor(results, currentPageNumber, hasMoreResults) {
        /** @type {Object[]} */
        this.results = results;

        /** @type {Number} */
        this.currentPageNumber = currentPageNumber;
        
        /** @type {Boolean} */
        this.hasMoreResults = hasMoreResults;
    } 
}