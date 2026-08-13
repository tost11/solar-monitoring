import * as React from 'react';
import {useContext, useState} from 'react';
import AppBar from "@mui/material/AppBar";
import Button from "@mui/material/Button";
import FormControl from "@mui/material/FormControl";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import "./main.css"
import {Login, UserContext} from './context/UserContext';
import LoginComponent from './Component/LoginComponent';
import Menu from './Menu';
import RegistrationView from './views/RegistrationView';
import {useTranslation} from "react-i18next";
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
        <Toolbar className={"MenuBar"} style={{paddingTop:"4px",paddingBottom:"4px"}}>
          <div>
            {login && (
              <Typography  variant="h6">
                {t("common.username")}:  {login.name+(login.admin?" (Admin)":"")}
              </Typography>
            )}
          </div>
          <div className={"defaultFlexNoRowGap"} style={{rowGap:"4px"}}>
            <div style={{marginLeft:"auto"}}>
              <FormControl className="Input">
                <Select
                  sx={{ maxHeight: "40px" }}
                  value={i18n.language}
                  onChange={(ev)=>{
                    i18n.changeLanguage(ev.target.value)
                  }}
                >
                  <MenuItem key={"de"} value={"de"}><span className="smallContent" data-small="de" data-big={t("languages.german")}></span></MenuItem>
                  <MenuItem key={"en"} value={"en"}><span className="smallContent" data-small="en" data-big={t("languages.english")}></span></MenuItem>
                </Select>
              </FormControl>
            </div>

            {login &&
              <div style={{marginLeft:"auto",width:"64px"}}>
                <div style={{display:"grid"}}>
                  <Menu setLogin={setLogin}/>
                </div>
              </div>
            }{!login && (
              <>
                <div style={{marginLeft:"auto"}}>
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
                </div>
                <div style={{marginLeft:"auto"}}>
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
                </div>
                <div style={{marginLeft:"auto",width:"64px"}}>
                    <Menu setLogin={setLogin}/>
                </div>
              </>
            )}
          </div>
        </Toolbar>
      </AppBar>
      <LoginComponent open={loginIsOpen} onClose={()=>setLoginIsOpen(false)} setLogin={setLogin} />
    <RegistrationView open={registerIsOpen} onClose={()=>setRegisterIsOpen(false)} setLogin={setLogin}/>
    </div>

}
