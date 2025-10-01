import UrlHelper from '../utils/UrlHelper';
import { isStateless, isMsalStateless, isCloudflareStateless, getCloudflareLocalJwtAssertion } from '../auth/AuthFlags';
import TokenProvider from '../auth/TokenProvider';

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
     * CSRF token for stateful mode only.
     */
    getCSRF() {
        return APP_CONFIG.csrfToken;
    }

    /**
     * Build headers based on auth mode, HTTP method and content type.
     * - Stateless (MSAL): include Authorization; include Content-Type for JSON payloads only
     * - Stateless (Cloudflare): include optional CF header override; include Content-Type for JSON payloads only
     * - Stateful: include X-CSRF-TOKEN for non-GET; include Content-Type for JSON payloads only
     *
     * @param {string} method HTTP method (GET, POST, PUT, PATCH, DELETE)
     * @param {boolean} isBinary true to omit Content-Type (binary uploads)
     * @returns {Promise<Object>} headers object
     */
    buildHeaders(method, isBinary = false) {
        const stateless = isStateless();

        if (stateless) {
            if (isCloudflareStateless()) {
                const headers = {};
                if (!isBinary) headers['Content-Type'] = 'application/json';
                const localAssertion = getCloudflareLocalJwtAssertion();
                if (localAssertion) {
                    headers['CF-Access-Jwt-Assertion'] = localAssertion;
                }
                return Promise.resolve(headers);
            }

            if (isMsalStateless()) {
                return TokenProvider.getAccessToken().then(token => {
                    const headers = {};
                    if (!isBinary) headers['Content-Type'] = 'application/json';
                    headers['Authorization'] = `Bearer ${token}`;
                    return headers;
                });
            }
        } else {
            const headers = {};
            if (method !== 'GET') {
                if (!isBinary) headers['Content-Type'] = 'application/json';
                headers['X-CSRF-TOKEN'] = this.getCSRF();
            }
            return Promise.resolve(headers);
        }

        return Promise.resolve({});
    }

    /**
     * Returns the appropriate fetch credentials mode based on auth mode.
     * - Stateless: 'omit' (no cookies)
     * - Stateful: 'include' (send session cookies)
     */
    getCredentialsMode() {
        return isStateless() ? 'omit' : 'include';
    }

    get(url, data) {
        return this.buildHeaders('GET').then(headers =>
            fetch(url + '?' + UrlHelper.toQueryString(data), {
                follow: 0,
                credentials: this.getCredentialsMode(),
                headers: headers
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
                return response.json();
            })
        );
    }

    put(url, data) {
        return this.buildHeaders('PUT').then(headers =>
            fetch(url, {
                method: 'put',
                compress: false, // workaround for node-fetch, see this file header
                credentials: this.getCredentialsMode(),
                body: JSON.stringify(data),
                headers: headers,
                follow: 0
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
            })
        );
    }

    putBinaryData(url, data) {
        return this.buildHeaders('PUT', true).then(headers =>
            fetch(url, {
                method: 'put',
                compress: false, // workaround for node-fetch, see this file header
                credentials: this.getCredentialsMode(),
                body: data,
                headers: headers,
                follow: 0
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
            })
        );
    }

    post(url, data) {
        return this.buildHeaders('POST').then(headers =>
            fetch(url, {
                method: 'post',
                compress: false, // workaround for node-fetch, see this file header
                credentials: this.getCredentialsMode(),
                body: JSON.stringify(data),
                headers: headers,
                follow: 0
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
                return response.json();
            })
        );
    }

    patch(url, data) {
        return this.buildHeaders('PATCH').then(headers =>
            fetch(url, {
                method: 'PATCH',
                compress: false, // workaround for node-fetch, see this file header
                credentials: this.getCredentialsMode(),
                body: JSON.stringify(data),
                headers: headers,
                follow: 0
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
            })
        );
    }

    delete(url) {
        return this.buildHeaders('DELETE').then(headers =>
            fetch(url, {
                method: 'delete',
                credentials: this.getCredentialsMode(),
                headers: headers,
                follow: 0
            }).then(response => {
                this.handleUnauthenticatedResponse(response);
            })
        );
    }

}

export default BaseClient;
