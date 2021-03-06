import React from "react";
import {Button, Dialog, DialogActions, DialogTitle} from '@mui/material';
import {Login} from "../context/UserContext"
import {useNavigate} from "react-router-dom";

interface LogoutProps {
  setLogin: (login?:Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LogoutProps) {
  let navigate = useNavigate()
  return  <div>
    <Dialog
    open={open}
    onClose={()=>onClose}
    aria-labelledby="draggable-dialog-title"
  >
    <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
      What you really Logout?
    </DialogTitle>

    <DialogActions>
      <Button autoFocus onClick={()=>{
        onClose()}
      } color="primary">
        Cancel
      </Button>
      <Button onClick={()=>{
        setLogin(undefined)
        onClose()
        navigate("/")
      }
      } color="primary">
        Logout
      </Button>
    </DialogActions>
  </Dialog>
</div>
}
