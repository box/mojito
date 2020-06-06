import rootReducer from "./rootReducer"
import {configureStore} from '@reduxjs/toolkit'

const initialState = {
    repository: {
        repositories: [
            {id: 1, name: "projectA", timestamp: Date.now()},
            {id: 2, name: "projectB", timestamp: Date.now()}
        ]
    }
}

const createStore = () => configureStore({
    reducer: rootReducer,
    preloadedState: initialState
})

export default createStore