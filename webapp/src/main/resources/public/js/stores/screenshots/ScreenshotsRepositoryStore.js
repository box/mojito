import alt from "../../alt";
import ScreenshotsRepositoryActions from "../../actions/screenshots/ScreenshotsRepositoryActions";
import ScreenshotsRepositoryDataSource from "../../actions/screenshots/ScreenshotsRepositoryDataSource";
import ScreenshotsPageActions from "../../actions/screenshots/ScreenshotsPageActions";
import RepositoryLocale from "../../sdk/entity/RepositoryLocale";

class ScreenshotsRepositoryStore {

    constructor() {
        this.setDefaultState();
        this.bindActions(ScreenshotsRepositoryActions);
        this.bindActions(ScreenshotsPageActions);
        this.registerAsync(ScreenshotsRepositoryDataSource);
    }
    
    setDefaultState() {
        this.repositories = [];
        this.selectedRepositoryIds = [];
        this.dropdownOpen = false;
    }
    
    resetScreenshotSearchParams() {
        this.setDefaultState();
    }
    
    getAllRepositories() {
        this.getInstance().getAllRepositories();
    }
    
    getAllRepositoriesSuccess(repositories) {
        this.repositories = repositories;
    }

    changeSelectedRepositoryIds(selectedRepositoryIds) {
        this.selectedRepositoryIds = selectedRepositoryIds.slice().sort();
    }
    
    changeDropdownOpen(dropdownOpen) { 
        this.dropdownOpen = dropdownOpen;
    }

    /**
     * Get the repository with the id from the state.
     * @param id of repository
     * @return {object} the repository with the id.  null if not found.
     */
    static getRepositoryById(id) {
        let state = this.getState();
        let result = null;

        for (let key of Object.keys(state.repositories)) {
            if (state.repositories[key].id === id) {
                result = state.repositories[key];
                break;
            }
        }

        return result;
    }

    /**
     * Get all BCP47 tags that belongs to a list of repositories
     * @param {Repository[]} repositories list of repositories
     * @param {Boolean} filteredByFullyTranslated [Optional] True to return only fully translated
     * @return {string[]}
     */
    static getAllBcp47TagsForRepositories(repositories, filteredByFullyTranslated = false) {

        let bcp47Tags = [];

        repositories.forEach(repository => {
            if (repository) {
                repository.repositoryLocales.forEach(repositoryLocale => {
                    if (!RepositoryLocale.isRootLocale(repositoryLocale)) {
                        if (filteredByFullyTranslated) {
                            if (repositoryLocale.toBeFullyTranslated) {
                                bcp47Tags[repositoryLocale.locale.bcp47Tag] = null;
                            }
                        } else {
                            bcp47Tags[repositoryLocale.locale.bcp47Tag] = null;
                        }
                    }
                });
            }
        });

        return Object.keys(bcp47Tags);
    }

    /**
     * Get all BCP47 tags that belongs to a repository
     * @param {number} repositoryId
     * @return {string[]}
     */
    static getAllBcp47TagsForRepo(repositoryId) {
        let repository = this.getRepositoryById(repositoryId);
        return this.getAllBcp47TagsForRepositories([repository]);
    }

    /**
     * Get all BCP47 tags that are to be fully translated that belongs to a repository
     * @param {number} repositoryId
     * @return {string[]}
     */
    static getAllToBeFullyTranslatedBcp47TagsForRepo(repositoryId) {
        let repository = this.getRepositoryById(repositoryId);
        return this.getAllBcp47TagsForRepositories([repository], true);
    }

    /**
     * Get all BCP47 tags that belongs to a list of repositories
     * @param {object} rlist of epositoryId
     * @param {Boolean} filteredByFullyTranslated [Optional] True to return only fully translated
     * @return {string[]}
     */
    static getAllBcp47TagsForRepositoryIds(repositoryIds, filteredByFullyTranslated = false) {

        let repositories = [];

        repositoryIds.forEach(repositoryId => repositories.push(this.getRepositoryById(repositoryId)));

        return this.getAllBcp47TagsForRepositories(repositories, filteredByFullyTranslated);
    }

    /**
     * Get all BCP47 tags for all repositories
     * @return {string[]}
     */
    static getAllBcp47TagsForAllRepositories() {
        return this.getAllBcp47TagsForRepositories(this.getState().repositories);
    }
}

export default alt.createStore(ScreenshotsRepositoryStore, 'ScreenshotsRepositoryStore');
