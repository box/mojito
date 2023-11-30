import BaseClient from "./BaseClient";

class ClobStorageClient extends BaseClient {

    getClob(uuid) {
        return this.get(this.getUrl(uuid));
    }

    saveClob(content) {
        return this.post(this.getUrl(), content);
    }

    getEntityName() {
        return 'clobstorage';
    }
}
;

export default new ClobStorageClient();



