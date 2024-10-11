export default class UserSearcherParameters {
    constructor() {
        this.params = {};
    }

    search(searchText) {
        this.params.search = searchText;
        return this;
    }

    page(page) {
        this.params.page = page;
        return this;
    }

    size(size) {
        this.params.size = size;
        return this;
    }

    getParams() {
        return this.params;
    }
}
