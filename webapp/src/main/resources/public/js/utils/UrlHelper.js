class UrlHelper {

    constructor() {
        this.contextPath = APP_CONFIG.contextPath;
    }

    getUrlWithContextPath(url) {
        return this.contextPath + url;
    }

    toQueryString(params) {
        const encodedParams = [];
        for (const key in params)
            if (params.hasOwnProperty(key)) {
                const value = params[key];
                if (value instanceof Array) {
                    const arrayKey = key + '[]';
                    value.forEach(v => encodedParams.push(encodeURIComponent(arrayKey) + '=' + encodeURIComponent(v)));
                } else if (value !== null) {
                    encodedParams.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
                }
            }
        return encodedParams.join("&");
    }
};

export default new UrlHelper();