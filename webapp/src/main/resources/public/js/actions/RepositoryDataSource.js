import RepositoryClient from "../sdk/RepositoryClient.js";
import RepositoryActions from "./RepositoryActions.js";

const RepositoryDataSource = {
    createRepository: {
        remote(state, repository) {
            return RepositoryClient.createRepository(repository);
        },

        success: RepositoryActions.createRepositorySuccess,
        error: RepositoryActions.createRepositoryError
    },

    getAllRepositories: {
        remote() {
            return RepositoryClient.getRepositories();
        },

        success: RepositoryActions.getAllRepositoriesSuccess,
        error: RepositoryActions.getAllRepositoriesError
    }
};

export default RepositoryDataSource;
