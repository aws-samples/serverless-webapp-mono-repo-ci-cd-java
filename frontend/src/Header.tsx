import React from 'react';
import {useHistory} from "react-router";
import {makeStyles} from '@material-ui/core/styles';
import MenuIcon from '@material-ui/icons/Menu';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import {
    AppBar,
    Divider,
    Drawer,
    IconButton, List,
    ListItem,
    ListItemText,
    Toolbar,
    Typography
} from "@material-ui/core";

import {Links_List} from "./GlobalConstants";
import {Home} from "@material-ui/icons";

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    title: {
        flexGrow: 1,
    },
    drawerHeader: {
        display: 'flex',
        alignItems: 'center',
        padding: theme.spacing(0, 1),
        // necessary for content to be below app bar
        ...theme.mixins.toolbar,
        justifyContent: 'space-between',
    },
}));

export default function ButtonAppBar() {
    const classes = useStyles();
    const [anchorEl, setAnchorEl] = React.useState<boolean>(false);
    const history = useHistory();

    const openMenu = (event: any) => {
        setAnchorEl(event.currentTarget);
    };

    const closeMenu = () => {
        setAnchorEl(false);
    };

    const redirectToLink = (link: string) => {
        closeMenu();
        window.open(link);
    };

    const redirectToHome = () => {
        history.push('/');
    };

    return (
        <div className={classes.root}>
            <AppBar position="static">
                <Toolbar>
                    <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu">
                        <MenuIcon onClick={openMenu}/>
                        <Drawer
                            anchor='left'
                            open={anchorEl}
                            onClose={closeMenu}>
                            <div className={classes.drawerHeader}>
                                <p className='drawer-header'>Helpful Links</p>
                                <IconButton onClick={closeMenu}>
                                    <ChevronLeftIcon />
                                </IconButton>
                            </div>
                            <Divider />
                            <List>
                            {Links_List.map((link, i) => {
                                return (
                                <div className='drawer-item' key={i}>
                                    <ListItem button onClick={() => redirectToLink(link.link)}>
                                        <ListItemText primary={link.label} />
                                    </ListItem>
                                </div>
                            )})}
                            </List>
                        </Drawer>
                    </IconButton>
                    <Typography variant="h6" className={classes.title}>
                        Facial Recognition
                    </Typography>
                    <IconButton color="inherit" onClick={redirectToHome}>
                        <Home />
                    </IconButton>
                </Toolbar>
            </AppBar>
        </div>
    );
}