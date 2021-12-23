import React, {useContext, useState} from "react";
import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Input, Modal} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import {postLogin} from "../api/LoginAPI";

interface LogoutProps {
  setLogin: (login:Login|null) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LogoutProps) {

  const login = useContext(UserContext);
  const doLogin = postLogin();

  return  <div>
    <Dialog
    open={open}
    onClose={()=>onClose}
    aria-labelledby="draggable-dialog-title"
  >
    <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
      What you realy Logout?
    </DialogTitle>

    <DialogActions>
      <Button autoFocus onClick={()=>{
        onClose()}
      } color="primary">
        Cancel
      </Button>
      <Button onClick={()=>{
        setLogin(null)
        onClose()
      }
      } color="primary">
        Logout
      </Button>
    </DialogActions>
  </Dialog>
</div>
}
