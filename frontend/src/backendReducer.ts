type Action = {
    type: string,
    payload: string
}

function backendReducer(state: string, action: Action) {
    switch (action.type) {
        case 'SET_BACKEND':
            state = action.payload
            console.log(state)
            return state;
        default:
            return state
    }
}

export {backendReducer};