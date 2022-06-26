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

interface MenuProps {
  setLogin : (login?:Login)=> void;
}

export default function MenuBar({setLogin}:MenuProps) {
  const [loginIsOpen,setLoginIsOpen] = useState(false)
  const [registerIsOpen,setRegisterIsOpen] = useState(false)
  const login = useContext(UserContext);
  let navigate = useNavigate()

  return <div>
      <AppBar position="static">
        <Toolbar className={"MenuBar"}>

          <div>
            {login && (
              <Typography  variant="h6">
                User:  {login.name+(login.admin?" (Admin)":"")}
              </Typography>
            )}
          </div>

          <div className={"MenuBox"}>

            {login &&
              <Menu setLogin={setLogin}/>
          }{!login && (
              <div>
                <Button
                  variant="contained"
                  onClick={()=>setLoginIsOpen(true)}
                >Login
                </Button>
                <Button
                  variant="contained"
                  onClick={()=>setRegisterIsOpen(true)}
                >Register
                </Button>
                <Button
                  sx={{
                    color: "white"
                  }}
                  onClick={()=>navigate("/")}
                >Home
                </Button>
              </div>
            )}
          </div>

        </Toolbar>
      </AppBar>
      <LoginComponent open={loginIsOpen} onClose={()=>setLoginIsOpen(false)} setLogin={setLogin} />
    <RegistrationView open={registerIsOpen} onClose={()=>setRegisterIsOpen(false)} setLogin={setLogin}/>
    </div>

}
