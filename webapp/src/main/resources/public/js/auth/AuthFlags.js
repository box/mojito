export function isStateless() {
    try {
        return !!(typeof APP_CONFIG !== 'undefined'
            && APP_CONFIG.stateless
            && APP_CONFIG.stateless.enabled === true);
    } catch (e) {
        return false;
    }
}

