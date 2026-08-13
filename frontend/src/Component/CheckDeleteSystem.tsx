import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogTitle from "@mui/material/DialogTitle";
import {deleteSystem} from "../api/SolarSystemAPI";
import React from "react";
import {useTranslation} from "react-i18next";

interface CheckDeleteSystemProps {
  onClose:()=>void;
  open:boolean;
  systemId:number
}

export default function CheckDeleteSystem({onClose,open,systemId}:CheckDeleteSystemProps){

  var { t } = useTranslation();

  return<div>
    <Dialog
      open={open}
      onClose={()=>onClose}
      aria-labelledby="draggable-dialog-title"
    >
      <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
        {t("components.solarsystem.delete")}
      </DialogTitle>

      <DialogActions>
        <Button autoFocus onClick={()=>{
          onClose()}
        } color="primary">
          {t("common.yes")}
        </Button>
        <Button onClick={() => {
          deleteSystem(systemId).then(onClose)
        }
        } color="primary">
          {t("common.yes")}
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
