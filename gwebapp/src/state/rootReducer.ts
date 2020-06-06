import {combineReducers} from "redux"


import {systemReducer} from './system/reducers'
import {repositoryReducer} from './repository/reducers'

const rootReducer = combineReducers({
    system: systemReducer,
    repository: repositoryReducer
})

export type RootState = ReturnType<typeof rootReducer>
export default rootReducer



export function selectRepositories(state) {
    return state.repository.repositories;
}
