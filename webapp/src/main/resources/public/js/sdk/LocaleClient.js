import BaseClient from "./BaseClient.js";

class LocaleClient extends BaseClient {
    getLocales() {
        return this.get(this.getUrl(), {});
    }

    getEntityName() {
        return 'locales';
    }
};

export default new LocaleClient();