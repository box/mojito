import BaseClient from "./BaseClient";

class RepositoryClient extends BaseClient {

    getRepositories() {
        return this.get(this.getUrl(), {});
    }

    getEntityName() {
        return 'repositories';
    }
};

export default new RepositoryClient();