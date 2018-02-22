import Ict from "./Ict";
import "../../../sass/ict.scss";

var enMessages = require(`../../../properties/en.properties`);
var ict = new Ict(enMessages, 'en');

var config = {
    enabled: false,
    mojitoBaseUrl: '',
    removeTagsBlock: true
};

chrome.storage.sync.get(config, (items) => {
    config = items;
    if (items.enabled) {
        ict.activate();
        ict.setMojitoBaseUrl(items.mojitoBaseUrl);
        ict.setRemoveTagsBlock(items.removeTagsBlock);
    }
});

chrome.storage.onChanged.addListener((changes) => {
    for (var key in changes) {
        config[key] = changes[key].newValue;

        if (key === 'enabled' && config[key]) {
            ict.activate();
        }

        if (key === 'mojitoBaseUrl') {
            ict.setMojitoBaseUrl(config.mojitoBaseUrl);
        }

        if (key === 'removeTagsBlock') {
            ict.setRemoveTagsBlock(config.removeTagsBlock);
        }
    }
});

chrome.extension.onMessage.addListener(function(request, sender, sendResponse) {
    console.log('message listener');
    if (request.name === 'prepareScreenshot') {
        ict.processScreenshots();
    }
    return true;
});