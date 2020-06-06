import {SystemActionTypes, SystemState, UPDATE_SESSION} from './types'

export function updateSession(newSession: SystemState): SystemActionTypes {
    return {
        type: UPDATE_SESSION,
        payload: newSession
    }
}