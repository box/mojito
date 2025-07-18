import BaseClient from "./BaseClient.js";

class RepositoryClient extends BaseClient {
    createRepository(repository) {
        return this.post(this.getUrl(), repository);
    }

    getRepositories() {
        return this.get(this.getUrl(), {});
    }

    getEntityName() {
        return 'repositories';
    }
};

export default new RepositoryClient();