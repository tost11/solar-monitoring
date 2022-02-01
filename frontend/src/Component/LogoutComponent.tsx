import React from "react";
import {Button, Dialog, DialogActions, DialogTitle} from '@mui/material';
import {Login} from "../context/UserContext"

interface LogoutProps {
  setLogin: (login:Login|null) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LogoutProps) {

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
