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
import { useContext } from 'react';
import {Login, UserContext } from './UserContext';


interface MenuProps {
  setLogin : ( login:Login)=> void;
}

export default function MenuAppBar({setLogin}:MenuProps) {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const login = useContext(UserContext);

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <Box>
      <AppBar>
        <Toolbar className={"MenuBar"}>

          <Typography  variant="h6">
            Photos
          </Typography>

          <div className={"MenuBox"}>
          {login && (
            <div>
              <IconButton
                size="large"
                aria-label="account of current user"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                color="inherit"
              >
                <AccountCircle />
              </IconButton>
              <Menu
                id="menu-appbar"
                anchorEl={anchorEl}
                anchorOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                keepMounted
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
                open={Boolean(anchorEl)}
                onClose={handleClose}
              >

                <MenuItem onClick={handleClose}>Profile</MenuItem>
                <MenuItem onClick={handleClose}>My account</MenuItem>

              </Menu>
            </div>
          )}{!login && (
              <div>
                <Button
                  variant="contained"
                onChange={setLogin}>
                  Login
                </Button>
              </div>
            )}
          <IconButton
            size="large"
            edge="start"
            color="inherit"
            aria-label="menu">
            <MenuIcon />
          </IconButton>
          </div>

        </Toolbar>
      </AppBar>
    </Box>
  );
}
