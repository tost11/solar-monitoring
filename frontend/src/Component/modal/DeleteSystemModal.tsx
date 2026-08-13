import React from "react";
import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogTitle from "@mui/material/DialogTitle";
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {apiDeleteSystem} from "../../api/UserAPIFunctions";

interface LogoutProps {
  onClose:()=>void;
  open:boolean;
  systemId:string;
}

export default function DeleteSystemModal({onClose,open,systemId}: LogoutProps) {

  const { t } = useTranslation()

  const logout = ()=>{
    onClose()
    navigate("/")
  }

  const deleteUser = ()=>{
    apiDeleteSystem(systemId).then(logout)
  }

  let navigate = useNavigate()
  return <div>
    <Dialog
    open={open}
    onClose={()=>onClose}
    aria-labelledby="draggable-dialog-title">
      <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
        {t("components.delete_system_modal.delete_question")}
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
