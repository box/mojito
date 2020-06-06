import {ADD_REPOSITORY, DELETE_REPOSITORY, REPLACE_REPOSITORIES, RepositoryActionTypes, RepositoryState,} from './types'

const initialState: RepositoryState = {
    repositories: []
}

export function repositoryReducer(
    state = initialState,
    action: RepositoryActionTypes
): RepositoryState {
    switch (action.type) {
        case ADD_REPOSITORY:
            return {
                repositories: [...state.repositories, action.payload]
            }
        case DELETE_REPOSITORY:
            return {
                repositories: state.repositories.filter(
                    repository => repository.timestamp !== action.meta.timestamp
                )
            }
        case REPLACE_REPOSITORIES:
            return action.payload
        default:
            return state
    }
}
