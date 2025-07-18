
import BoxSDKConfigActions from "../../actions/boxsdk/BoxSDKConfigActions.js";
import BoxSDKConfigClient from "../../sdk/BoxSDKConfigClient.js";

const BoxSDKConfigDataSource = {

    getConfig: {
        remote() {
            return BoxSDKConfigClient.getConfig().then(result => {
                return result;
            });
        },
        success: BoxSDKConfigActions.getConfigSuccess,
        error: BoxSDKConfigActions.getConfigError
    },

    setConfig: {
        /**
         * @param {BoxSDKConfigStore} boxSDKConfigState
         * @param {BoxSDKConfig} boxSDKConfig
         * @return {Promise.<PollableTaskFuture>}
         */
        remote(boxSDKConfigState, boxSDKConfig) {
            return BoxSDKConfigClient.setConfig(boxSDKConfig);
        },
        success: BoxSDKConfigActions.setConfigSuccess,
        error: BoxSDKConfigActions.setConfigError
    }
};

export default BoxSDKConfigDataSource;
