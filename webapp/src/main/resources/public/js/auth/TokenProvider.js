import {PublicClientApplication, InteractionRequiredAuthError} from '@azure/msal-browser';
import {isStateless} from './AuthFlags';

let msalInstance = null;
let msalReadyPromise = null;

function ensureMsalReady() {
    if (!isStateless()) return Promise.resolve(null);

    if (!msalInstance) {
        const config = (APP_CONFIG.stateless && APP_CONFIG.stateless.msal) || {};
        const redirectUri = location.origin + APP_CONFIG.contextPath + '/auth/callback';
        msalInstance = new PublicClientApplication({
            auth: {
                authority: config.authority,
                clientId: config.clientId,
                redirectUri: redirectUri,
                navigateToLoginRequestUrl: false
            },
            cache: {
                cacheLocation: 'localStorage',
                storeAuthStateInCookie: false
            }
        });
    }

    if (!msalReadyPromise) {
        msalReadyPromise = msalInstance.initialize().then(() => msalInstance);
    }
    return msalReadyPromise;
}

function getScopeArray() {
    return [AP_CONFIG?.stateless?.msal?.scope];
}

const TokenProvider = {
    handleRedirect() {
        return ensureMsalReady().then(instance => {
            if (!instance) return null;
            return instance.handleRedirectPromise().then(result => {
                if (result && result.account) {
                    instance.setActiveAccount(result.account);
                } else {
                    const accounts = instance.getAllAccounts();
                    if (accounts.length > 0) instance.setActiveAccount(accounts[0]);
                }
                return result ? {account: result.account, state: result.state} : null;
            });
        });
    },

    login(returnTo) {
        return ensureMsalReady().then(instance => {
            if (!instance) return Promise.reject(new Error('TokenProvider.login called in stateful mode'));

            const state = returnTo || (location.pathname.substr(APP_CONFIG.contextPath.length) + location.search);
            instance.loginRedirect({
                scopes: getScopeArray(),
                state: JSON.stringify({returnTo: state})
            });
            return null;
        });
    },

    logout() {
        return ensureMsalReady().then(instance => {
            if (!instance) return Promise.reject(new Error('TokenProvider.logout called in stateful mode'));
            const account = instance.getActiveAccount();
            return instance.logoutRedirect({account});
        });
    },

    getAccessToken() {
        if (!isStateless()) {
            return Promise.reject(new Error('TokenProvider called in stateful mode'));
        }

        return ensureMsalReady().then(instance => {

            let account = instance.getActiveAccount();
            if (!account) {
                const accounts = instance.getAllAccounts();
                if (accounts.length > 0) {
                    account = accounts[0];
                    instance.setActiveAccount(account);
                }
            }

            if (!account) {
                // Not logged in yet, trigger login redirect to current page
                return this.login().then(() => new Promise(() => {
                }));
            }

            return instance.acquireTokenSilent({scopes: getScopeArray(), account: account}).then(result => {
                return result.accessToken;
            }).catch(err => {
                if (err instanceof InteractionRequiredAuthError) {
                    // Fall back to redirect flow if user interaction is needed
                    return this.login().then(() => new Promise(() => {
                    }));
                }
                return Promise.reject(err);
            });
        });
    }
};

export default TokenProvider;
