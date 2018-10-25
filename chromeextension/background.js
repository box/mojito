var backgroundConfig = {
    mojitoBaseUrl: 'http://localhost:8080/',
    enabled: false,
    headerName: 'X-Mojito-Ict',
    headerValue: 'on',
    actionButtons: []
};

chrome.storage.sync.get(backgroundConfig, (items) => {
    backgroundConfig = items;
    chrome.storage.sync.set(backgroundConfig);
});

chrome.storage.onChanged.addListener((changes) => {
    for (var key in changes) {
        backgroundConfig[key] = changes[key].newValue;
    }
});

function addMojitoIctHeader(details) {
    if (backgroundConfig.enabled && backgroundConfig.headerName && backgroundConfig.headerName !== '') {
       
        var hasUpdatedHeader = false;

        for (var i = 0; i < details.requestHeaders.length; ++i) {
            if (details.requestHeaders[i].name === backgroundConfig.headerName) {
                details.requestHeaders.value = details.requestHeaders.value + backgroundConfig.headerValue;
                hasUpdatedHeader = true;
                break;
            }
        }

        if (!hasUpdatedHeader) {
            details.requestHeaders.push({name: backgroundConfig.headerName, value: backgroundConfig.headerValue});
        }
    }
    return {requestHeaders: details.requestHeaders};
}

chrome.webRequest.onBeforeSendHeaders.addListener(
        addMojitoIctHeader,
        {urls: ['<all_urls>']},
        ['requestHeaders', 'blocking']
        );
