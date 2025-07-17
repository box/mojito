// Converts "extension:checkerType,..." string to an array of objects [{assetExtension, integrityCheckerType}]
export function deserializeAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return [];
    return assetIntegrityCheckersString.split(',').map(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return {
            assetExtension: assetExtension && assetExtension.trim(),
            integrityCheckerType: integrityCheckerType && integrityCheckerType.trim()
        };
    });
}

// Validates that assetIntegrityCheckersString is in the format "extension:checkerType,..."
export function validateAssetIntegrityCheckers(assetIntegrityCheckersString) {
    if (!assetIntegrityCheckersString) return true;
    return assetIntegrityCheckersString.split(',').every(pair => {
        const [assetExtension, integrityCheckerType] = pair.trim().split(':');
        return Boolean(assetExtension) && Boolean(integrityCheckerType);
    });
}

// Converts [{parentLocale, childLocales, toBeFullyTranlsated}...] to
// [{childLocale, parentLocale, toBeFullyTranslated}...]
export function flattenRepositoryLocales(repositoryLocales) {
    const result = [];
    const childLocalesSet = new Set();

    repositoryLocales.forEach(parent => {
        if (Array.isArray(parent.childLocales)) {
            parent.childLocales.forEach(child => {
                childLocalesSet.add(child.locale);
            });
        }
    });

    repositoryLocales.forEach(parent => {
        if (!childLocalesSet.has(parent.locale)) {
            result.push({
                locale: parent.locale,
                toBeFullyTranslated: parent.toBeFullyTranslated
            });
        }

        if (Array.isArray(parent.childLocales)) {
            parent.childLocales.forEach(child => {
                result.push({
                    locale: child.locale,
                    toBeFullyTranslated: child.toBeFullyTranslated,
                    parentLocale: { locale: parent.locale }
                });
            });
        }
    });

    return result;
}