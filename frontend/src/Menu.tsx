import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import SwipeableDrawer from "@mui/material/SwipeableDrawer";
import Typography from "@mui/material/Typography";
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
          <List>
            <ListItemButton className={"LogoutButton"} onClick={() =>{
                navigate("/")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.home")}/>
            </ListItemButton>
            <ListItemButton className={"LogoutButton"} onClick={() =>{
                navigate("/systems")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.system_list")}/>
            </ListItemButton>
            {login && <ListItemButton className={"LogoutButton"} onClick={() =>{
                navigate("/user")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.profile_settings")}/>
            </ListItemButton>}
            {login && <ListItemButton className={"LogoutButton"} onClick={() =>{
                navigate("/createNewSystem")
                setMenuIsOpen(false)
              }}>
              <ListItemText primary={t("components.menu.create_system")}/>
            </ListItemButton>}
            {login && login.admin && <ListItemButton className={"LogoutButton"} onClick={() =>{
              navigate("/Settings")
            }}>
              <ListItemText primary={t("common.settings")}/>
            </ListItemButton>}
            {login && login.admin && <ListItemButton className={"LogoutButton"} onClick={() =>{
              navigate("/tags")
            }}>
              <ListItemText primary={t("components.menu.tag_settings")}/>
            </ListItemButton>}
            {login && <ListItemButton className={"LogoutButton"} onClick={() =>{
              setMenuIsOpen(false);
              setIsLogoutOpen(true);
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.logout")}/>
            </ListItemButton>}
          </List>
        </div>
        <div>
          <List>
            <ListItemButton className={"LogoutButton"} onClick={() =>{
              navigate("/privacypolicy")
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.privacy_policy")}/>
            </ListItemButton>
            <ListItemButton className={"LogoutButton"} onClick={() =>{
              navigate("/impressum")
              setMenuIsOpen(false)
            }}>
              <ListItemText primary={t("common.imprint")}/>
            </ListItemButton>
          </List>
        </div>
      </div>
    </SwipeableDrawer>
    <LogoutComponent open={isLogoutOpen} onClose={() => setIsLogoutOpen(false)} setLogin={setLogin}/>
  </>
}
