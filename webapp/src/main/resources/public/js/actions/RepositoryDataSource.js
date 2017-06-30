import RepositoryClient from "../sdk/RepositoryClient";
import RepositoryActions from "./RepositoryActions";

const RepositoryDataSource = {
    getAllRepositories: {
        remote() {
            return RepositoryClient.getRepositories();
        },

        success: RepositoryActions.getAllRepositoriesSuccess,
        error: RepositoryActions.getAllRepositoriesError
    }
};

export default RepositoryDataSource;
