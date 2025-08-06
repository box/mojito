import { PublicClientApplication, InteractionRequiredAuthError } from '@azure/msal-browser';
import { isStateless } from './AuthFlags';

let msalInstance = null;
let msalReadyPromise = null;

function ensureMsalReady() {
    if (!isStateless()) return Promise.resolve(null);

    if (!msalInstance) {
        const cfg = (APP_CONFIG.stateless && APP_CONFIG.stateless.msal) || {};
        const redirectUri = location.origin + APP_CONFIG.contextPath + '/auth/callback';
        msalInstance = new PublicClientApplication({
            auth: {
                authority: cfg.authority,
                clientId: cfg.clientId,
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

function getScopesArray() {
    const scope = APP_CONFIG?.stateless?.msal?.scope;
    if (!scope) return [];
    if (Array.isArray(scope)) return scope;
    if (typeof scope === 'string') {
        return scope.split(/[\s,]+/).filter(Boolean);
    }
    return [];
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
                return result ? { account: result.account, state: result.state } : null;
            });
        });
    },

    login(returnTo) {
        return ensureMsalReady().then(instance => {
            if (!instance) return Promise.reject(new Error('TokenProvider.login called in stateful mode'));

            const state = returnTo || (location.pathname.substr(APP_CONFIG.contextPath.length) + location.search);
            const scopes = getScopesArray();

            instance.loginRedirect({
                scopes: scopes,
                state: JSON.stringify({ returnTo: state })
            });
            return null;
        });
    },

    logout() {
        return ensureMsalReady().then(instance => {
            if (!instance) return Promise.reject(new Error('TokenProvider.logout called in stateful mode'));
            const account = instance.getActiveAccount();
            return instance.logoutRedirect({ account });
        });
    },

    getAccessToken() {
        if (!isStateless()) {
            return Promise.reject(new Error('TokenProvider called in stateful mode'));
        }

        return ensureMsalReady().then(instance => {
            const scopes = getScopesArray();

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
                return this.login().then(() => new Promise(() => {}));
            }

            return instance.acquireTokenSilent({ scopes, account }).then(result => {
                return result.accessToken;
            }).catch(err => {
                if (err instanceof InteractionRequiredAuthError) {
                    // Fall back to redirect flow if user interaction is needed
                    return this.login().then(() => new Promise(() => {}));
                }
                return Promise.reject(err);
            });
        });
    }
};

export default TokenProvider;
