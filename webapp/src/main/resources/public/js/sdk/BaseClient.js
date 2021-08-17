import UrlHelper from '../utils/UrlHelper';

class BaseClient {
    constructor() {
        this.baseUrl = location.origin + APP_CONFIG.contextPath +  '/api/';
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
        return APP_CONFIG.csrfToken;
    }

    getHeaders() {
        return {
            'X-CSRF-TOKEN': this.getCSRF(),
            'Content-Type': 'application/json'
        };
    }

    get(url, data) {
        return fetch(url + '?' + UrlHelper.toQueryString(data), {
            follow: 0,
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

    putBinaryData(url, data) {
        const redirectMethod = APP_CONFIG.security.handleRedirectAsErrorOnImageUpload === true ? "manual" : "follow"
        return fetch(url, {
            method: 'put',
            compress: false, // workaround for node-fetch, see this file header
            credentials: 'include',
            body: data,
            headers: {
                'X-CSRF-TOKEN': this.getCSRF()
            },
            follow: 0,
            redirect: redirectMethod
        }).then(response => {
            if (APP_CONFIG.security.handleRedirectAsErrorOnImageUpload === true && response.status === 0) {
                // Workaround for OAuth redirect for auth on image upload causing silent TOO_MANY_REDIRECTS failure
                BaseClient.authenticateHandler();
            }
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
            credentials: 'include',
            headers: this.getHeaders(),
            follow: 0
        }).then(response => {
            this.handleUnauthenticatedResponse(response);
        });
    }

}

export default BaseClient;
