import BranchStatisticsContent from "./BranchStatisticsContent";


export default class BranchStatistics {
    constructor() {
        this.hasNext = false;
        this.size = 0;
        this.hasPrevious = false;
        this.number = 0;
        this.first = true;
        this.numberOfElements = 1;
        this.totalPages = 1;
        this.totalElements = 1;
        this.last = true;
        this.content = null;
    }

    static toBranchStatistics(json) {
        const result = new BranchStatistics();

        if (json) {
            result.hasNext = json.hasNext;
            result.size = json.size;
            result.hasPrevious = json.hasPrevious;
            result.number = json.number;
            result.first = json.first;
            result.numberOfElements = json.numberOfElements;
            result.totalPages = json.totalPages;
            result.totalElements = json.totalElements;
            result.last = json.last;
            this.content = BranchStatisticsContent.toContentList(json.content);
        }


        return result;
    }
}