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
import {useTranslation} from "react-i18next";
import {FormControl} from "@mui/material";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";

interface MenuProps {
  setLogin : (login?:Login)=> void;
}

export default function MenuBar({setLogin}:MenuProps) {

  const { t, i18n } = useTranslation();

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

          <div className={"defaultFlex"}>
            <FormControl className="Input">
              <InputLabel className="Input">{t("components.time_range.duration")}</InputLabel>
              <Select
                value={i18n.language}
                onChange={(ev)=>{
                  i18n.changeLanguage(ev.target.value)
                }}
              >
                <MenuItem key={"de"} value={"de"}>{t("languages.german")}</MenuItem>
                <MenuItem key={"en"} value={"en"}>{t("languages.english")}</MenuItem>
              </Select>
            </FormControl>

            <div style={{margin:"auto"}} className={"MenuBox"}>

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
          </div>

        </Toolbar>
      </AppBar>
      <LoginComponent open={loginIsOpen} onClose={()=>setLoginIsOpen(false)} setLogin={setLogin} />
    <RegistrationView open={registerIsOpen} onClose={()=>setRegisterIsOpen(false)} setLogin={setLogin}/>
    </div>

}
