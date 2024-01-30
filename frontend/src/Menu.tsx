import {IconButton, List, ListItem, ListItemText, SwipeableDrawer, Typography} from "@mui/material";
import MenuIcon from '@mui/icons-material/Menu';
import {useNavigate} from 'react-router-dom';

import React, {useContext, useState} from "react";
import LogoutComponent from "./Component/LogoutComponent";
import {Login, UserContext} from "./context/UserContext";

interface LogoutProps {
  setLogin: (login?: Login) => void;
}

export default function Menu({setLogin}:LogoutProps) {
  const [menuIsOpen, setMenuIsOpen] = useState(false)
  const [isLogoutOpen, setIsLogoutOpen] = useState(false)
  const navigate = useNavigate()

  const login = useContext(UserContext);

  return <div>
    <IconButton
        aria-label="account of current user"
        aria-controls="menu-appbar"
        aria-haspopup="true"
        color="inherit"
        onClick={() => setMenuIsOpen(true)}
    >
      <MenuIcon/>
    </IconButton>
      <SwipeableDrawer
        anchor={"right"}
        open={menuIsOpen}
        onClose={() => setMenuIsOpen(false)}
        onOpen={() => setMenuIsOpen(true)}
      >
        <Typography variant="h6">Menu</Typography>
        <List sx={{display:"flex", alignItems:"flex-end",flexDirection:"column",}}>
          <ListItem button key={"Home"} className={"LogoutButton"} onClick={() =>{
              navigate("/")
              setMenuIsOpen(false)
            }}>
            <ListItemText primary={"Home"}/>
          </ListItem>
          <ListItem button key={"Show all System"} className={"LogoutButton"} onClick={() =>{
              navigate("/systems")
              setMenuIsOpen(false)
            }}>
            <ListItemText primary={"Show all System"}/>
          </ListItem>
          {login && <ListItem button key={"Profile Settings"} className={"LogoutButton"} onClick={() =>{
              navigate("/user")
              setMenuIsOpen(false)
            }}>
            <ListItemText primary={"Profile Settings"}/>
          </ListItem>}
          {login && <ListItem button key={"Add a new SolarSystem"} className={"LogoutButton"} onClick={() =>{
              navigate("/createNewSystem")
              setMenuIsOpen(false)
            }}>
            <ListItemText primary={"Add a new SolarSystem"}/>
          </ListItem>}
          {login && <ListItem button key={"Logout"} className={"LogoutButton"} onClick={() =>{
              setMenuIsOpen(false);
              setIsLogoutOpen(true);
              setMenuIsOpen(false)
            }}>
            <ListItemText primary={"Logout"}/>
          </ListItem>}
          {login && login.admin && <ListItem button key={"Settings"} onClick={() =>{
            navigate("/Settings")
          }}>
            <ListItemText primary={"Settings"}/>
          </ListItem>}
        </List>
      </SwipeableDrawer>
    <LogoutComponent open={isLogoutOpen} onClose={() => setIsLogoutOpen(false)} setLogin={setLogin}/>
  </div>
}
