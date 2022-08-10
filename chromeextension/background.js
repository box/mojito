var backgroundConfig = {
    mojitoBaseUrl: 'http://localhost:8080/',
    enabled: false,
    headerName: 'X-Mojito-Ict',
    headerValue: 'on',
    actionButtons: [],
    mtEndpointUrlFormat: ''
};

function addMojitoIctHeader() {
    // First removes any pre-existing rule with id 1, then adds a new one with same id. The remove
    // occurs before the addition operation as documented at
    // https://developer.chrome.com/docs/extensions/reference/declarativeNetRequest/#method-updateDynamicRules
    chrome.declarativeNetRequest.updateDynamicRules({
        addRules:[{
            "id": 1,
            "priority": 1,
            "action":  {
                "type": "modifyHeaders",
                "requestHeaders": [
                    {"header": backgroundConfig.headerName, "operation": "set", "value": backgroundConfig.headerValue}
                ]
            },
            "condition": {"urlFilter":  "*", "resourceTypes":  ["main_frame"]}
        }],
        removeRuleIds: [1]
    });
}

function removeMojitoIctHeader() {
    chrome.declarativeNetRequest.updateDynamicRules({
        removeRuleIds: [1]
    });
}

chrome.storage.sync.get(backgroundConfig, (items) => {
    backgroundConfig = items;
    chrome.storage.sync.set(backgroundConfig);
});

chrome.storage.onChanged.addListener((changes) => {
    for (var key in changes) {
        backgroundConfig[key] = changes[key].newValue;
        if (key === "enabled") {
            if (backgroundConfig.enabled) {
                addMojitoIctHeader();
            } else {
                removeMojitoIctHeader();
            }
        }
        else if (key === "headerName" || key === "headerValue") {
            if (backgroundConfig.enabled) {
                addMojitoIctHeader();
            }
        }
    }
});

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

