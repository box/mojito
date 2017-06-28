class UrlHelper {

    constructor() {
        this.contextPath = CONTEXT_PATH;
    }

    getUrlWithContextPath(url) {
        return this.contextPath + url;
    }
};

export default new UrlHelper();