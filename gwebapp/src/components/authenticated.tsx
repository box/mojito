import * as React from "react"
import {useSelector} from "react-redux";
import {RootState} from "../state/rootReducer";
import {navigate} from "@reach/router";

const Authenticated = ({component: Component, ...rest }) => {
    const system = useSelector((state: RootState) => state.system)

    const location = rest.location

    if (!system.loggedIn && location.pathname !== `/login`) {
        console.log(location)
        navigate("/login?redirect=" + location.pathname + location.search)
        return null
    }

    return <Component {...rest} />
}


export default Authenticated
