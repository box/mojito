import * as React from "react"
import {RouteComponentProps} from "@reach/router";
import Repository from "./repository";
import {useDispatch, useSelector} from "react-redux";
import {RootState} from "../state/rootReducer";
import {addRepository, replaceRepositories} from "../state/repository/actions";
import Layout from "./layout";
import {fetchAndReplaceRepositories} from "../state/repository/thunks";

const RepositoryList = ({repositories}) => (
    <ul>
        {repositories.map(repository => (
            <Repository key={repository.id} {...repository}
                        onClick={(e) => console.log("Click repository: ", repository.id, e.currentTarget)}/>
        ))}
    </ul>
)

const selectRepositories = (state: RootState) => state.repository.repositories

const ConnectRepositoryList = () => {
    const repositories = useSelector(selectRepositories)
    return <RepositoryList repositories={repositories}/>
}

export default function Search(_: RouteComponentProps) {

    const dispatch = useDispatch();

    const repositories = useSelector(selectRepositories)

    return <Layout>
        <button onClick={() => {
            const repositoryId = repositories.length + 1;
            dispatch(addRepository({
                id: repositoryId,
                name: "Repository-" + repositoryId,
                timestamp: Date.now()
            }))
        }}>
            Add repository
        </button>


        <button onClick={() => {
            const repositoryId = repositories.length + 1;
            dispatch(replaceRepositories(
                {
                    repositories: [{
                        id: 1,
                        name: "Repository-" + 1,
                        timestamp: Date.now()
                    }]
                }))
        }}>
            Reset repositories
        </button>


        <button onClick={() => {
            dispatch(fetchAndReplaceRepositories())
        }}>
            Fetch repositories
        </button>
        <ConnectRepositoryList/>
    </Layout>
}
