import {replaceRepositories} from "./actions";

export const fetchAndReplaceRepositories = () => {
    return (dispatch) => {
        fetchrepositories().then(repos => {
            dispatch(replaceRepositories(
                {
                    repositories: repos.map(repo => {
                        return {
                            id: repo.id,
                            name: repo.name,
                            timestamp: repo.createdAt
                        }
                    })
                }))
        })
    }
}

// move to another file
const fetchrepositories = async () => {
    const url = 'api/repositories';
    const response = await fetch(url);
    const json = await response.json();
    return json
}
