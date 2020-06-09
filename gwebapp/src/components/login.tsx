import * as React from "react"
import {navigate, RouteComponentProps} from "@reach/router";
import {useDispatch, useSelector} from "react-redux";
import {updateSession} from "../state/system/actions";
import {RootState} from "../state/rootReducer";
import {Link} from "gatsby";

const LoggedIn = () => {
    const dispatch = useDispatch();
    return <div onClick={() => {
        dispatch(updateSession({
            loggedIn: false,
            userName: "",
            session: ""
        }))
    }}>Click here to logout
    </div>
}

const NotLoggedIn = ({navigateTo}) => {

    const dispatch = useDispatch();

    return <><div onClick={() => {
        dispatch(updateSession({
            loggedIn: true,
            userName: "Jano",
            session: "no-session"
        }))
        navigate(navigateTo)
    }}>Click here to login
    </div>
        <Link to="http://localhost:8080/login/oauth2/authorization/github">Github</Link>
        </>
}

export default function Login(_: RouteComponentProps) {
    const system = useSelector((state: RootState) => state.system)
    console.log(_.location)

    const navigateToArray = _.location.search.split("redirect=")
    const navigateTo = navigateToArray.length == 2 ? navigateToArray[1] : "/"
    console.log(navigateTo)
    return system.loggedIn ? <LoggedIn/> : <NotLoggedIn navigateTo={navigateTo}/>
}
