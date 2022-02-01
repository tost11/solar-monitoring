import {IconButton, List, ListItem, ListItemText, SwipeableDrawer, Typography} from "@mui/material";
import MenuIcon from '@mui/icons-material/Menu';
import {Login} from "../context/UserContext";
import {useNavigate} from 'react-router-dom';

import React, {useState} from "react";
import LogoutComponent from "./LogoutComponent";

interface LogoutProps {
  setLogin: (login: Login | null) => void;
}

export default function MenuComponent({setLogin}: LogoutProps) {
  const [menuIsOpen, setMenuIsOpen] = useState(false)
  const [isLogoutOpen, setIsLogoutOpen] = useState(false)
  let navigate = useNavigate()


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

    <React.Fragment>
      <SwipeableDrawer
          anchor={"right"}
          open={menuIsOpen}
          onClose={() => setMenuIsOpen(false)}
          onOpen={() => setMenuIsOpen(true)}
      >
        <Typography variant="h6">Menu</Typography>
        <List>

          {['Home', 'Starred', 'Show all System', 'Add a new SolarSystem', 'Settings', 'Test', 'Logout'].map((text, index) => (
              <ListItem button key={text} onClick={() => {
                if (text == "Home") {
                  navigate("/")
                }
                if (text == "Starred") {
                  navigate("")

                }
                if (text == "Show all System") {
                  navigate("/system")

                }
                if (text == "Add a new SolarSystem") {
                  navigate("/createNewSystem")
                }
                if (text == "Test") {
                  navigate("/test")
                }

                if (text == "Logout") {
                  console.log("logout");
                  setMenuIsOpen(false);
                  setIsLogoutOpen(true);
                }
                setMenuIsOpen(false)
              }
              }>
                {/*add icons <ListItemIcon>*/}
                <ListItemText primary={text}/>
              </ListItem>
          ))}
        </List>
      </SwipeableDrawer>
    </React.Fragment>
    <LogoutComponent open={isLogoutOpen} onClose={() => setIsLogoutOpen(false)} setLogin={setLogin}/>
  </div>
}
