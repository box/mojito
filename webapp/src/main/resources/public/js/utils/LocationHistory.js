import UrlHelper from "./UrlHelper";

class LocationHistory {

    /**
     * Updates the browser location for a given pathname and query params
     *
     * If the URL is only the pathname replace the history state
     * (to reflect the params) else if the query has changed
     * push a new state to keep track of the change param modification.
     *
     * @param {string} pathname the pathname of the url to be processed
     * @param {object} params params to build the query string
     */
    updateLocation(router, pathname, params) {
        if (window.location.pathname === UrlHelper.getUrlWithContextPath(pathname)) {
            const newQuery = this.buildQuery(params);

            if (window.location.search === "") {
                router.replace(pathname + "?" + newQuery, null, null);
            } else if (!this.isCurrentQueryEqual("?" + newQuery)) {
                router.push(pathname + "?" + newQuery, null, null);
            }
        }
    }

    /**
     * @param {string} queryString Starts with ?
     * @return boolean
     */
    isCurrentQueryEqual(queryString) {
        return queryString === window.location.search;
    }

    /**
     * Create query string given params
     *
     * @param params
     * @return {*}
     */
    buildQuery(params) {
        const cloneParam = _.clone(params);
        delete cloneParam["changedParam"]; // this is workbench specific
        return UrlHelper.toQueryString(cloneParam);
    }
};

export default new LocationHistory();



