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
import {FormControl, Icon} from "@mui/material";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import LoginIcon from '@mui/icons-material/Login';
import AppRegistrationIcon from '@mui/icons-material/AppRegistration';

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
              <Select
                sx={{ maxHeight: "40px" }}
                value={i18n.language}
                onChange={(ev)=>{
                  i18n.changeLanguage(ev.target.value)
                }}
              >
                <MenuItem key={"de"} value={"de"}><span class="smallContent" data-small="de" data-big={t("languages.german")}></span></MenuItem>
                <MenuItem key={"en"} value={"en"}><span class="smallContent" data-small="en" data-big={t("languages.english")}></span></MenuItem>
              </Select>
            </FormControl>

            <div style={{margin:"auto"}} className={"MenuBox"}>

              {login &&
                <Menu setLogin={setLogin}/>
              }{!login && (
                <div className={"flexRow"} style={{gap:"5px"}}>
                  <Button
                    sx={{
                      '& .button-label': {
                        display: {
                          xs: 'none',
                          sm: 'inline', // anzeigen ab 'sm' (600px+)
                        },
                      },
                    }}
                    startIcon={<LoginIcon/>}
                    variant="contained"
                    onClick={()=>setLoginIsOpen(true)}
                  >
                    <span className="button-label">{t("common.login")}</span>
                  </Button>
                  <Button
                    sx={{
                      '& .button-label': {
                        display: {
                          xs: 'none',
                          sm: 'inline', // anzeigen ab 'sm' (600px+)
                        },
                      },
                    }}
                    startIcon={<AppRegistrationIcon/>}
                    variant="contained"
                    onClick={()=>setRegisterIsOpen(true)}
                  ><span className="button-label">{t("common.register")}</span>
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
