import React from "react";
import {Button, Dialog, DialogActions, DialogTitle} from '@mui/material';
import {Login} from "../../context/UserContext"
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {apiLogout} from "../../api/UserAPIFunctions";

interface LogoutProps {
  setLogin: (login?:Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LogoutProps) {

  const { t } = useTranslation()

  const logout = ()=>{
    setLogin(undefined)
    onClose()
    navigate("/")
  }

  const doApiLogout = ()=>{
    //even if fail logout key will be still valid but client don't know him
    apiLogout().then(logout).catch(logout)
  }

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
        <Button onClick={doApiLogout} color="primary">
          {t("common.logout")}
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
