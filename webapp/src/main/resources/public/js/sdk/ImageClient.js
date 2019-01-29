import BaseClient from "./BaseClient";

class ImageClient extends BaseClient {

    getImage(params) {
        return this.get(this.getUrl(), params);
    }

    uploadImage(generatedUuid, imageContent) {
        return this.put(this.getUrl() + '/' + generatedUuid, imageContent);
    }

    getEntityName() {
        return 'images';
    }
}

export default new ImageClient();