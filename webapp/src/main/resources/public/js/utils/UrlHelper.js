class UrlHelper {

    constructor() {
        this.contextPath = APP_CONFIG.contextPath;
    }

    getUrlWithContextPath(url) {
        return this.contextPath + url;
    }

    toQueryString(params) {
        let encodedParams = [];
        for (const key in params)
            if (params.hasOwnProperty(key)) {
                let value = params[key];
                if (value instanceof Array) {
                    let arrayKey = key + '[]';
                    value.forEach(v => encodedParams.push(encodeURIComponent(arrayKey) + '=' + encodeURIComponent(v)));
                } else if (value !== null) {
                    encodedParams.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
                }
            }
        return encodedParams.join("&");
    }
};

export default new UrlHelper();