import {IconButton, List, ListItem, ListItemText, SwipeableDrawer, Typography} from "@mui/material";
import MenuIcon from '@mui/icons-material/Menu';
import {useNavigate} from 'react-router-dom';

import React, {useContext, useState} from "react";
import LogoutComponent from "./Component/modal/LogoutComponent";
import {Login, UserContext} from "./context/UserContext";
import {useTranslation} from "react-i18next";

interface LogoutProps {
  setLogin: (login?: Login) => void;
}

export default function Menu({setLogin}:LogoutProps) {

  const { t } = useTranslation();

  const [menuIsOpen, setMenuIsOpen] = useState(false)
  const [isLogoutOpen, setIsLogoutOpen] = useState(false)
  const navigate = useNavigate()

  const login = useContext(UserContext);

  return <>
    <div style={{display:"grid"}}>
      <IconButton
          aria-label="account of current user"
          aria-controls="menu-appbar"
          aria-haspopup="true"
          color="inherit"
          onClick={() => setMenuIsOpen(true)}
      >
        <MenuIcon/>
      </IconButton>
    </div>
    <SwipeableDrawer
      anchor={"right"}
      open={menuIsOpen}
      onClose={() => setMenuIsOpen(false)}
      onOpen={() => setMenuIsOpen(true)}
    >
      <div style={{justifyContent:"space-between",height:"100%"}} className="defaultFlowColumn">
        <div>
          <Typography variant="h6">{t("common.menu")}</Typography>
          <List sx={{display:"flex", alignItems:"flex-end",flexDirection:"column",}}>
            <ListItem button className={"LogoutButton"} onClick={() =>{
                navigate("/")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.home")}/>
            </ListItem>
            <ListItem button className={"LogoutButton"} onClick={() =>{
                navigate("/systems")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.system_list")}/>
            </ListItem>
            {login && <ListItem button className={"LogoutButton"} onClick={() =>{
                navigate("/user")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.profile_settings")}/>
            </ListItem>}
            {login && <ListItem button className={"LogoutButton"} onClick={() =>{
                navigate("/createNewSystem")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.create_system")}/>
            </ListItem>}
            {login && login.admin && <ListItem button onClick={() =>{
              navigate("/Settings")
            }}>
              <ListItemText primary={t("common.settings")}/>
            </ListItem>}
            {login && login.admin && <ListItem button onClick={() =>{
              navigate("/tags")
            }}>
              <ListItemText primary={t("components.menu.tag_settings")}/>
            </ListItem>}
            {login && <ListItem button className={"LogoutButton"} onClick={() =>{
              setMenuIsOpen(false);
              setIsLogoutOpen(true);
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.logout")}/>
            </ListItem>}
          </List>
        </div>
        <div>
          <List sx={{display:"flex", alignItems:"flex-end",flexDirection:"column",}}>
            <ListItem button className={"LogoutButton"} onClick={() =>{
              navigate("/privacypolicy")
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.privacy_policy")}/>
            </ListItem>
            <ListItem button className={"LogoutButton"} onClick={() =>{
              navigate("/impressum")
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.imprint")}/>
            </ListItem>
          </List>
        </div>
      </div>
    </SwipeableDrawer>
    <LogoutComponent open={isLogoutOpen} onClose={() => setIsLogoutOpen(false)} setLogin={setLogin}/>
  </>
}
