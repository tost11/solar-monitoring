import * as React from 'react';
import {useContext, useState} from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import "./main.css"
import {Login, UserContext} from './context/UserContext';
import LoginComponent from './Component/LoginComponent';
import MenuComponent from './Component/MenuComponent';
import RegisterComponent from './Component/RegisterComponent';

interface MenuProps {
  setLogin : (login:Login|null)=> void;
}

export default function MenuBar({setLogin}:MenuProps) {
  const [loginIsOpen,setLoginIsOpen] = useState(false)
  const [registerIsOpen,setRegisterIsOpen] = useState(false)
  const login = useContext(UserContext);

  return <div>
      <AppBar position="static">
        <Toolbar className={"MenuBar"}>

          <div>
            {login && (
              <Typography  variant="h6">
                User:  {login?.name}
              </Typography>
            )}
          </div>

          <div className={"MenuBox"}>

            {login &&
              <MenuComponent setLogin={setLogin}/>
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
              </div>
            )}
          </div>

        </Toolbar>
      </AppBar>
      <LoginComponent open={loginIsOpen} onClose={()=>setLoginIsOpen(false)} setLogin={setLogin} />
    <RegisterComponent open={registerIsOpen} onClose={()=>setRegisterIsOpen(false)} setLogin={setLogin}/>
    </div>

}
