import React from "react";
import {Button, Dialog, DialogActions, DialogTitle} from '@mui/material';
import {Login} from "../../context/UserContext"
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {apiAddTagToSystem, apiDeleteUser, apiLogout} from "../../api/UserAPIFunctions";

interface LogoutProps {
  setLogin: (login?:Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function DeleteUserModal({setLogin,onClose,open}: LogoutProps) {

  const { t } = useTranslation()

  const logout = ()=>{
    setLogin(undefined)
    onClose()
    navigate("/")
  }

  const deleteUser = ()=>{
    apiDeleteUser().then(logout)
  }

  let navigate = useNavigate()
  return <div>
    <Dialog
    open={open}
    onClose={()=>onClose}
    aria-labelledby="draggable-dialog-title">
      <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
        {t("components.delete_user_modal.delete_question")}
      </DialogTitle>

      <DialogActions>
        <Button onClick={deleteUser} color="primary">
          {t("common.yes")}
        </Button>
        <Button autoFocus onClick={()=>{
          onClose()}
        } color="primary">
          {t("common.no")}
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
