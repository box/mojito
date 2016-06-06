import alt from "../../alt";

import BoxSDKConfig from "../../sdk/entity/BoxSDKConfig";
import BoxSDKConfigActions from "../../actions/boxsdk/BoxSDKConfigActions";
import BoxSDKConfigDataSource from "./BoxSDKConfigDataSource";

class BoxSDKConfigStore {

    constructor() {

        /** @type {BoxSDKConfig} */
        this.boxSDKConfig;

        this.bindActions(BoxSDKConfigActions);

        this.registerAsync(BoxSDKConfigDataSource);
    }

    /**
     */
    onGetConfig() {
        this.getInstance().getConfig();
    }

    /**
     * @param {BoxSDKConfig} result
     */
    onGetConfigSuccess(result) {
        this.boxSDKConfig = result;
    }

    onGetConfigError() {
        
    }

    /**
     * @param {BoxSDKConfig} boxSDKConfig
     */
    onSetConfig(boxSDKConfig) {
        this.getInstance().setConfig(boxSDKConfig);
    }

    onSetConfigSuccess() {

    }

    onSetConfigError() {

    }
}

export default alt.createStore(BoxSDKConfigStore, 'BoxSDKConfigStore');

