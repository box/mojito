import {
    ADD_REPOSITORY,
    DELETE_REPOSITORY,
    REPLACE_REPOSITORIES,
    Repository,
    RepositoryActionTypes,
    RepositoryState
} from './types'

export function addRepository(newRepository: Repository): RepositoryActionTypes {
    return {
        type: ADD_REPOSITORY,
        payload: newRepository
    }
}

export function deleteRepository(timestamp: number): RepositoryActionTypes {
    return {
        type: DELETE_REPOSITORY,
        meta: {
            timestamp
        }
    }
}

export function replaceRepositories(state: RepositoryState): RepositoryActionTypes {
    return {
        type: REPLACE_REPOSITORIES,
        payload: state
    }
}