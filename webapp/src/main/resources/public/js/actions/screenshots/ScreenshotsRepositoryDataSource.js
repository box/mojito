import RepositoryClient from "../../sdk/RepositoryClient.js";
import ScreenshotsRepositoryActions from "./ScreenshotsRepositoryActions.js";

const ScreenshotsRepositoryDataSource = {
    getAllRepositories: {
        remote() {
            return RepositoryClient.getRepositories();
        },

        success: ScreenshotsRepositoryActions.getAllRepositoriesSuccess,
        error: ScreenshotsRepositoryActions.getAllRepositoriesError
    }
};

export default ScreenshotsRepositoryDataSource;
