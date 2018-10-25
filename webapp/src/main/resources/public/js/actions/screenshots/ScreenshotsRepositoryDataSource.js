import RepositoryClient from "../../sdk/RepositoryClient";
import ScreenshotsRepositoryActions from "./ScreenshotsRepositoryActions";

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
