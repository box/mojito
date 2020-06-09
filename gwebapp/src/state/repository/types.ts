export interface Repository {
    name: string,
    id: number,
    timestamp: number
}

export interface RepositoryState {
    repositories: Repository[]
}

export const ADD_REPOSITORY = 'ADD_REPOSITORY'
export const DELETE_REPOSITORY = 'DELETE_REPOSITORY'
export const REPLACE_REPOSITORIES = 'REPLACE_REPOSITORIES'
export const FETCH_REPOSITORIES = 'FETCH_REPOSITORIES'

interface AddRepositoryAction {
    type: typeof ADD_REPOSITORY
    payload: Repository
}

interface DeleteRepositoryAction {
    type: typeof DELETE_REPOSITORY
    meta: {
        timestamp: number
    }
}

interface ReplaceRepositoriesAction {
    type: typeof REPLACE_REPOSITORIES
    payload: RepositoryState
}

interface FetchRepositoriesAction {
    type: typeof FETCH_REPOSITORIES
}


export type RepositoryActionTypes = AddRepositoryAction | DeleteRepositoryAction | ReplaceRepositoriesAction | FetchRepositoriesAction

