import * as React from "react"
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import IconButton from "@material-ui/core/IconButton";
import MenuIcon from "@material-ui/icons/Menu";
import Typography from "@material-ui/core/Typography";
import {Link} from "gatsby";
import Button from "@material-ui/core/Button";
import {makeStyles} from "@material-ui/core/styles";

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

export default function Navbar() {
    const classes = useStyles();
    return <AppBar position="static">
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
            <Button color="inherit"> <Link to="/login">Login</Link></Button>
        </Toolbar>
    </AppBar>
}


