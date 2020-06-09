import rootReducer from "./rootReducer"
import {configureStore, getDefaultMiddleware} from '@reduxjs/toolkit'
import thunk from "redux-thunk";

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
    preloadedState: initialState,
    middleware: [...getDefaultMiddleware(), thunk]
})

export default createStore