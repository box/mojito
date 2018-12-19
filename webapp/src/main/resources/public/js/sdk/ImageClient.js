import BaseClient from "./BaseClient";

class ImageClient extends BaseClient {

    getImage(params) {
        return this.get(this.getUrl(), params);
    }

    uploadImage(imageName, imageContent) {
        return this.put(this.getUrl() + '/' + imageName, imageContent);
    }

    getEntityName() {
        return 'images';
    }
}

export default new ImageClient();