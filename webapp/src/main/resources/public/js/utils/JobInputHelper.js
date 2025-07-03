// Converts "en:en-US, fr: fr-FR" to [{key: 'en', value: 'en-US'}, {key: 'fr', value: 'fr-FR'}]
export const parseLocaleMappingString = (localeMappingString) => {
    if (!localeMappingString || typeof localeMappingString !== 'string') return [];
    return localeMappingString.split(',').map(pair => {
        const [key, value] = pair.split(':');
        return {
            key: key ? key.trim() : '',
            value: value ? value.trim() : ''
        };
    }).filter(pair => pair.key || pair.value);
}

// Converts array of {key, value} to "en:en-US, fr:fr-FR"
export const serializeLocaleMappingArray = (localeMappingArray) => {
    if (!Array.isArray(localeMappingArray)) return '';
    return localeMappingArray
        .filter(pair => pair.key && pair.value)
        .map(pair => `${pair.key}:${pair.value}`)
        .join(', ');
}

// Converts array of {key, value} to ["key=value", ...]
export const serializeOptionsArray = (optionsArray) => {
    if (!Array.isArray(optionsArray)) return [];
    return optionsArray
        .filter(pair => pair.key && pair.value)
        .map(pair => `${pair.key}=${pair.value}`);
}

// Converts ["key=value", ...] to array of {key, value}
export const parseOptionsArray = (optionsList) => {
    if (!Array.isArray(optionsList)) return [];
    return optionsList.map(option => {
        const [key, value] = option.split('=');
        return {
            key: key ? key.trim() : '',
            value: value ? value.trim() : ''
        };
    }).filter(pair => pair.key || pair.value);
}
