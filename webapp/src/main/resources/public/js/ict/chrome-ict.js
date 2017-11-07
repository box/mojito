import Ict from "./Ict";
import "../../../sass/ict.scss";

var enMessages = require(`../../../properties/en.properties`);
var ict = new Ict(enMessages, 'en');

var config = {
    enabled: false
}

chrome.storage.sync.get(config, (items) => {
    console.log(items);
    config = items;
    if (items.enabled) {
        ict.activate();
        ict.setMojitoBaseUrl(items.mojitoBaseUrl);
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
    }
});