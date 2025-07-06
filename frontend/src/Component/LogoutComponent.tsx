import React from "react";
import {Button, Dialog, DialogActions, DialogTitle} from '@mui/material';
import {Login} from "../context/UserContext"
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";

interface LogoutProps {
  setLogin: (login?:Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LogoutProps) {

  const { t } = useTranslation()

  let navigate = useNavigate()
  return <div>
    <Dialog
    open={open}
    onClose={()=>onClose}
    aria-labelledby="draggable-dialog-title">
      <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
        {t("components.session.logout-question")}
      </DialogTitle>

      <DialogActions>
        <Button autoFocus onClick={()=>{
          onClose()}
        } color="primary">
          {t("common.cancel")}
        </Button>
        <Button onClick={()=>{
          setLogin(undefined)
          onClose()
          navigate("/")
        }
        } color="primary">
          {t("common.logout")}
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
