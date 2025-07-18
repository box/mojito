import BaseClient from "./BaseClient.js";
import BoxSDKConfig from "../sdk/entity/BoxSDKConfig.js";
import PollableTaskFuture from "./entity/PollableTaskFuture.js";

class BoxSDKConfigClient extends BaseClient {

    /**
     * Gets the config
     * @return {Promise.<BoxSDKConfig>}
     */
    getConfig() {
        const promise = this.get(this.getUrl(), {});

        return promise.then(results => {
            let result = null;
            // NOTE we only care about one config right now
            if (results.length > 0) {
                result = BoxSDKConfig.toBoxSDKConfig(results[0]);
            }

            return result;
        });
    }

    /**
     * @param {BoxSDKConfig} boxSDKConfig
     * @return {Promise.<PollableTaskFuture>}
     */
    setConfig(boxSDKConfig) {
        const promise = this.post(this.getUrl(), boxSDKConfig);
        return promise.then((pollableTaskFuture) => {
            return PollableTaskFuture.toPollableTaskFuture(pollableTaskFuture);
        });
    }

    getEntityName() {
        return 'boxSDKServiceConfigs';
    }
}

export default new BoxSDKConfigClient();



