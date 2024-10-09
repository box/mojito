import BaseClient from "./BaseClient";
import Locale from "./entity/Locale";
import User from "./entity/User";
import UserPage from "./UsersPage";

class LocaleClient extends BaseClient {

    /**
     * @returns {Locale[]}
     */
    getAllLocales() {
        let promise = this.get(this.getUrl(), );

        return promise.then(function (result) {
            return Locale.toLocales(result);
        });
    }

    getEntityName() {
        return 'locales';
    }
}

export default new LocaleClient();
