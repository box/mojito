const AuthTypes = {
    MSAL: 'MSAL',
    CLOUDFLARE: 'CLOUDFLARE'
};

export function isStateless() {
    try {
        return !!(typeof APP_CONFIG !== 'undefined'
            && APP_CONFIG.stateless
            && APP_CONFIG.stateless.enabled === true);
    } catch (e) {
        return false;
    }
}

export function getStatelessAuthType() {
    if (!isStateless()) {
        return null;
    }

    try {
        const configuredType = APP_CONFIG?.stateless?.type;
        if (configuredType) {
            const upper = configuredType.toString().toUpperCase();
            if (upper in AuthTypes) {
                return AuthTypes[upper];
            }
            return upper;
        }
    } catch (e) {
        // fall through to default
    }
    return AuthTypes.MSAL;
}

export function isMsalStateless() {
    return getStatelessAuthType() === AuthTypes.MSAL;
}

export function isCloudflareStateless() {
    return getStatelessAuthType() === AuthTypes.CLOUDFLARE;
}

export function getCloudflareLocalJwtAssertion() {
    if (!isCloudflareStateless()) {
        return null;
    }

    try {
        return APP_CONFIG?.stateless?.cloudflare?.localJwtAssertion || null;
    } catch (e) {
        return null;
    }
}
