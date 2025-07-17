import RepositoryClient from "../sdk/RepositoryClient";
import RepositoryActions from "./RepositoryActions";

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
