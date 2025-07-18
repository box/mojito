import alt from "../alt.js";
import RepositoryActions from "../actions/RepositoryActions.js";
import WorkbenchActions from "../actions/workbench/WorkbenchActions.js";
import RepositoryDataSource from "../actions/RepositoryDataSource.js";
import RepositoryLocale from "../sdk/entity/RepositoryLocale.js";

class RepositoryStore {

    constructor() {
        this.repositories = [];
        this.error = null;
        this.bindActions(RepositoryActions);
        this.bindActions(WorkbenchActions);
        this.registerAsync(RepositoryDataSource);
    }

    createRepository(repository) {
        this.getInstance().createRepository(repository);
    }

    createRepositorySuccess(repository) {
        this.repositories = [...this.repositories, repository];
        this.setError(null);
    }

    createRepositoryError(error) {
        this.setError(error);
    }

    setError(error) {
        this.error = error;
    }

    getError() {
        return this.error;
    }

    getAllRepositories() {
        this.getInstance().getAllRepositories();
    }

    getAllRepositoriesSuccess(repositories) {
        this.repositories = repositories;
    }

    /**
     * Get the repository with the id from the state.
     * @param id of repository
     * @return {object} the repository with the id.  null if not found.
     */
    static getRepositoryById(id) {
        const state = this.getState();
        let result = null;

        for (const key of Object.keys(state.repositories)) {
            if (state.repositories[key].id === id) {
                result = state.repositories[key];
                break;
            }
        }

        return result;
    }

    /**
     * Get the repository with given name from the state.
     * @param name of repository
     * @return {object} the repository with the name.  null if not found.
     */
    static getRepositoryByName(name) {
        const state = this.getState();
        let result = null;

        for (const key of Object.keys(state.repositories)) {
            if (state.repositories[key].name === name) {
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

        const bcp47Tags = [];

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
        const repository = this.getRepositoryById(repositoryId);
        return this.getAllBcp47TagsForRepositories([repository]);
    }

    /**
     * Get all BCP47 tags that are to be fully translated that belongs to a repository
     * @param {number} repositoryId
     * @return {string[]}
     */
    static getAllToBeFullyTranslatedBcp47TagsForRepo(repositoryId) {
        const repository = this.getRepositoryById(repositoryId);
        return this.getAllBcp47TagsForRepositories([repository], true);
    }

    /**
     * Get all BCP47 tags that belongs to a list of repositories
     * @param {object} rlist of epositoryId
     * @param {Boolean} filteredByFullyTranslated [Optional] True to return only fully translated
     * @return {string[]}
     */
    static getAllBcp47TagsForRepositoryIds(repositoryIds, filteredByFullyTranslated = false) {

        const repositories = [];

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

export default alt.createStore(RepositoryStore, 'RepositoryStore');
