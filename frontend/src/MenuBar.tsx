import * as React from 'react';
import {useContext, useState} from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import "./main.css"
import {Login, UserContext} from './context/UserContext';
import LoginComponent from './Component/LoginComponent';
import Menu from './Menu';
import RegistrationView from './views/RegistrationView';
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";

interface MenuProps {
  setLogin : (login?:Login)=> void;
}

export default function MenuBar({setLogin}:MenuProps) {

  const { t } = useTranslation();

  const [loginIsOpen,setLoginIsOpen] = useState(false)
  const [registerIsOpen,setRegisterIsOpen] = useState(false)
  const login = useContext(UserContext)

  return <div>
      <AppBar position="static">
        <Toolbar className={"MenuBar"}>

          <div>
            {login && (
              <Typography  variant="h6">
                {t("common.username")}:  {login.name+(login.admin?" (Admin)":"")}
              </Typography>
            )}
          </div>

          <div className={"MenuBox"}>

            {login &&
              <Menu setLogin={setLogin}/>
            }{!login && (
              <div className={"flexRow"} style={{gap:"5px"}}>
                <Button
                  variant="contained"
                  onClick={()=>setLoginIsOpen(true)}
                >{t("common.login")}
                </Button>
                <Button
                  variant="contained"
                  onClick={()=>setRegisterIsOpen(true)}
                >{t("common.register")}
                </Button>
                <Menu setLogin={setLogin}/>
              </div>
            )}
          </div>

        </Toolbar>
      </AppBar>
      <LoginComponent open={loginIsOpen} onClose={()=>setLoginIsOpen(false)} setLogin={setLogin} />
    <RegistrationView open={registerIsOpen} onClose={()=>setRegisterIsOpen(false)} setLogin={setLogin}/>
    </div>

}
