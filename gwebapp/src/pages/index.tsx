import * as React from "react"
import {Router} from "@reach/router"
import {Link} from "gatsby"
import Login from "../components/login"
import Home from "../components/home";
import Search from "../components/search";
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import AppBar from '@material-ui/core/AppBar';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';
import Toolbar from '@material-ui/core/Toolbar';
import CssBaseline from '@material-ui/core/CssBaseline';
import {makeStyles} from '@material-ui/core/styles';


const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
    },
    menuButton: {
        marginRight: theme.spacing(1),
    },
    title: {
        flexGrow: 1,
    },
}));


export default function Index() {
    const classes = useStyles();

    return <>
        <CssBaseline />
        <AppBar position="static">
            <Toolbar>
                <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu">
                    <MenuIcon/>
                </IconButton>
                <Typography variant="h6" className={classes.title}>
                    <Link to="/" color="secondary">Home</Link>
                </Typography>
                <Typography variant="h6" className={classes.title}>
                    <Link to="/search" color="secondary">Search</Link>
                </Typography>
                <Button color="inherit"> <Link to="login">Login</Link></Button>
            </Toolbar>
        </AppBar>

        <Router basepath="/">
            <Home path="/"/>
            <Search path="/search"/>
            <Login path="/login"/>
        </Router>
    </>
}



