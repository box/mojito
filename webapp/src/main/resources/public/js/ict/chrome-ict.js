import Ict from "./Ict";
import "../../../sass/ict.scss";

var enMessages = require(`../../../properties/en.properties`);
var ict = new Ict(enMessages, 'en');

var config = {
    enabled: false,
    mojitoBaseUrl: '',
    actionButtons: [],
};

chrome.storage.sync.get(config, (items) => {
    config = items;
    if (items.enabled) {
        ict.activate();
        ict.setMojitoBaseUrl(items.mojitoBaseUrl);
        ict.setActionButtons(items.actionButtons);
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

        if (key === 'actionButtons') {
            ict.setActionButtons(config.actionButtons);
        }
    }
});