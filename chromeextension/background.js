var backgroundConfig = {
    mojitoBaseUrl: 'http://localhost:8080/',
    enabled: false,
    headerName: 'X-Mojito-Ict',
    headerValue: 'on',
    actionButtons: [],
    mtEndpointUrlFormat: ''
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
        details.requestHeaders.push({name: backgroundConfig.headerName, value: backgroundConfig.headerValue});
    }
    return {requestHeaders: details.requestHeaders};
}

chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (backgroundConfig.mtEndpointUrlFormat) {
        var message = request['textUnitVariant']
        var locale = request['locale']
        var storageKey = "I18N_MT_" + locale + "_" + message
        chrome.storage.local.get([storageKey], function (result){
            if (result[storageKey]) {
                // Retrieve translation from local storage
                sendResponse({data: result[storageKey]})
            } else {
                var requestUrl = backgroundConfig.mtEndpointUrlFormat.replace("{mt_locale}", locale).replace("{mt_message}", message);
                fetch(requestUrl)
                    .then(response => response.json())
                    .then(json => {
                        chrome.storage.local.set({[storageKey]: json.data}, function (){
                            // Store translation in local storage for future retrieval
                            sendResponse(json)
                        })
                    })
                    .catch(err => sendResponse({data: ''}));
            }
        })

        return true;
    } else {
        sendResponse({data: ''});
    }

});

chrome.webRequest.onBeforeSendHeaders.addListener(
    addMojitoIctHeader,
    {urls: ['<all_urls>']},
    ['requestHeaders', 'blocking','extraHeaders']
);
