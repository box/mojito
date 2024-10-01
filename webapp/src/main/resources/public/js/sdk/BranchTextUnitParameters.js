export default class BranchTextUnitParameters {
    constructor() {
        this.params = {};
    }

    page(page) {
        this.params.page = page;
    }

    size(size) {
        this.params.size = size;
    }

    getParams() {
        return this.params;
    }
}
