import * as React from "react"
import {navigate, Router} from "@reach/router"
import Login from "../components/login"
import Home from "../components/home";
import Search from "../components/search";
import CssBaseline from '@material-ui/core/CssBaseline';
import {useSelector} from "react-redux";
import {RootState} from "../state/rootReducer";
import Authenticated from "../components/Authenticated";

export default function Index() {
    const system = useSelector((state: RootState) =>
        state.system
    )

    return <>
        <CssBaseline/>
        <Router basepath="/">
            <Home path="/"/>
            <Authenticated path="/search" component={Search}/>
            <Login path="/login"/>
        </Router>
    </>
}



