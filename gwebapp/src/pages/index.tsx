import * as React from "react"
import {Router} from "@reach/router"
import {Link} from "gatsby"
import Login from "../components/login"
import Home from "../components/home";

export default function Index() {
    return <>
        <nav>
            <Link to="/">Home</Link>
            <Link to="login">Login</Link>
        </nav>
        <Router basepath="/">
            <Home path="/" />
            <Login path="/login"/>
        </Router>
    </>
}
