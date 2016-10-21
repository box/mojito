import $ from "jquery";
// TODO remove node-fetch which is only useful for older browser like Safari 9.
// Chrome, Firefox support fetch correctly.
//
// node-fetch causes some issue with compression ("invalid response body at:
// http://localhost:8080/ap…mit=10 reason: data error: incorrect header check",
// type: "system", errno: "Z_DATA_ERROR", code: "Z_DATA_ERROR"}).
//
// A counter intuitive workaround is to use compress: false
// we can remove it later when Safari 9 doesn't need to be supported
import fetch from "node-fetch";

class BaseClient {
    constructor() {
        //TODO configure this!!
        this.baseUrl = location.origin + '/api/';
    }

    /**
     * The resource name used to build the WS URL, to be overriden by extending classes in order to customize the WS
     * URL.
     *
     * @returns {string} the resource name
     */
    getResourceName() {
        return '';
    }

    /**
     * Gets the WS URL.
     *
     * @param {string} id Optional resource id can be used to build URL for POST, PUT and DELETE. For GET the parameter
     * is omitted.
     * @returns the WS URL
     */
    getUrl(id = null) {

        var url = this.baseUrl + this.getEntityName();

        if (id) {
            url += '/' + id;
        }

        return url;
    }

    /**
     * Override this to customize behavior to authenticate for your own application
     */
    authenticateHandler() {
        console.log('BaseClient.authenticateHandler is not yet implemented');
    }

    /**
     * Checks the response and see if it is unauthenticated.  If so, trigger the authenticationHandler.
     * @param {Response} response
     */
    handleUnauthenticatedResponse(response) {
        console.log(response.status);
        if (response.status === 401) {
            BaseClient.authenticateHandler();
        }

        // The Promise returned from fetch() won't reject on HTTP error status even
        // if the response is a HTTP 404 or 500. Instead, it will resolve normally,
        // and it will only reject on network failure, or if anything prevented the request
        // from completing.
        this.checkStatus(response);
    }

    checkStatus(response) {
        if (response.status >= 200 && response.status < 300) {
            return response;
        } else {
            let error = new Error(response.statusText);
            error.response = response;
            throw error;
        }
    }


    /**
     * TODO for now just copy the token from the global variable but we'll some better logic later
     */
    getCSRF() {
        return CSRF_TOKEN;
    }

    getHeaders() {
        return {
            'X-CSRF-TOKEN': this.getCSRF(),
            'Content-Type': 'application/json'
        };
    }

    get(url, data) {
        return fetch(url + '?' + $.param(data), {
            follow: 0,
            compress: false, // workaround for node-fetch, see this file header
            credentials: 'include' // this is required if using fetch from the browser, not needed with node-fetch
        }).then(response => {
            this.handleUnauthenticatedResponse(response);
            return response.json();
        });
    }

    put(url, data) {
        return fetch(url, {
            method: 'put',
            compress: false, // workaround for node-fetch, see this file header
            credentials: 'include',
            body: JSON.stringify(data),
            headers: this.getHeaders(),
            follow: 0
        }).then(response => {
            this.handleUnauthenticatedResponse(response);
        });
    }

    post(url, data) {
        return fetch(url, {
            method: 'post',
            compress: false, // workaround for node-fetch, see this file header
            credentials: 'include',
            body: JSON.stringify(data),
            headers: this.getHeaders(),
            follow: 0
        }).then(response => {
            this.handleUnauthenticatedResponse(response);
            return response.json();
        });
    }

    delete(url) {
        return fetch(url, {
            method: 'delete',
            compress: false, // workaround for node-fetch, see this file header
            credentials: 'include',
            headers: this.getHeaders(),
            follow: 0
        }).then(response => {
            this.handleUnauthenticatedResponse(response);
        });
    }

}

export default BaseClient;
