import * as React from 'react';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import AccountCircle from '@mui/icons-material/AccountCircle';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import MenuItem from '@mui/material/MenuItem';
import Menu from '@mui/material/Menu';
import Button from '@mui/material/Button';
import "./main.css"
import { useContext, useState } from 'react';
import {Login, UserContext } from './UserContext';
import Popper from '@mui/material/Popper';
import { Modal } from '@mui/material';
import LoginComponent from './Component/LoginComponent';
import MenuComponent from './Component/MenuComponent';
import { useNavigate } from 'react-router-dom';
import RegisterComponent from './Component/RegisterComponent';


interface MenuProps {
  setLogin : (login:Login|null)=> void;
}

export default function MenuBar({setLogin,}:MenuProps) {
  const [loginIsOpen,setLoginIsOpen] = useState(false)
  const [registerIsOpen,setRegisterIsOpen] = useState(false)
  const login = useContext(UserContext);
  let navigate=useNavigate()

  return <div>
      <AppBar position="static">
        <Toolbar className={"MenuBar"}>

          <Typography  variant="h6">

          </Typography>

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
