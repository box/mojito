var backgroundConfig = {
    mojitoBaseUrl: 'http://localhost:8080/',
    enabled: false,
    headerName: 'X-Mojito-Ict',
    headerValue: 'on',
    removeTagsBlock: true
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

function dataUrlToBlob(dataURL) {
    var dataUrlParts = dataURL.split(',');
    var type = dataUrlParts[0].split(':')[1].split(';')[0];

    var contentAsBytes = atob(dataUrlParts[1]);
    var arrayBufferView = new Uint8Array(contentAsBytes.length);

    for (var i = 0; i < contentAsBytes.length; i++) {
        arrayBufferView[i] = contentAsBytes.charCodeAt(i);
    }

    return new Blob([arrayBufferView], {type: type});
}


function cropImage(dataUrl, data, callback) {
    var image = new Image();

    image.onload = function () {
        var screenshot = data.screenshots[0];
        
        var canvas = document.createElement('canvas');
        canvas.width = screenshot.width;
        canvas.height = screenshot.height;
        var ctx = canvas.getContext('2d');
        
        ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
        
        for (var textUnit of screenshot.textUnits) {
            ctx.strokeRect(textUnit.x , textUnit.y, textUnit.width, textUnit.height);
        }
        
        callback(canvas.toDataURL());
    };

    image.src = dataUrl;
}

function onImageDataUrlReceived(dataUrl, data) {
   
    cropImage(dataUrl, data, function (croppedDataUrl) {
        var xhr = new XMLHttpRequest();
        xhr.open("PUT", 'http://localhost:8080/api/images/test.jpg', true);

        xhr.onreadystatechange = () => {
            if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
                alert('Image saved');
            }
        };

        sendWithCSRF(xhr, dataUrlToBlob(croppedDataUrl));
        
        var xhr = new XMLHttpRequest();
        xhr.open("POST", 'http://localhost:8080/api/screenshots', true);
        xhr.setRequestHeader('Content-Type', 'application/json');
        
        xhr.onreadystatechange = () => {
            if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
                alert('Screenshot run created');
            }
        };
//
//        sendWithCSRF(xhr, JSON.stringify({
//            repository: { id: 101 },
//            name: 'run1',
//            screenshots: [
//                {
//                    name: 'screen1',
//                    src: 'api/images/test.jpg',
//                    locale: {id: 57},
//                    textUnits: [
//                        {
//                            name: 'more',
//                            renderedTarget: 'repository'
//                        }
//                    ]
//                }
//            ]
//        }));
        console.log(data);
        sendWithCSRF(xhr, JSON.stringify(data));
        
    });
}


function sendWithCSRF(r, data) {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", 'http://localhost:8080/api/csrf-token', true);

    xhr.onreadystatechange = () => {
        if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
            r.setRequestHeader('X-CSRF-TOKEN', xhr.responseText);
            r.send(data);
        }
    };

    xhr.send(data);
}

function onTakeScreenshot(data) {
    chrome.tabs.captureVisibleTab(null, {}, (dataUrl) => onImageDataUrlReceived(dataUrl, data));
}

chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    console.log('message in background', request);
    if (request.name === 'takeScreenshot') {
        onTakeScreenshot(request.data);
    }
    return true;
});