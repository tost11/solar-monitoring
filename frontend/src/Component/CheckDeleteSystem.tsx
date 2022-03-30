import {Button, Dialog, DialogActions, DialogTitle} from "@mui/material";
import {deleteSystem} from "../api/SolarSystemAPI";
import React from "react";

interface CheckDeleteSystemProps {
  onClose:()=>void;
  open:boolean;
  systemId:number
}

export default function CheckDeleteSystem({onClose,open,systemId}:CheckDeleteSystemProps){


  return<div>
    <Dialog
      open={open}
      onClose={()=>onClose}
      aria-labelledby="draggable-dialog-title"
    >
      <DialogTitle style={{ cursor: 'move' }} id="draggable-dialog-title">
        What you really delete this System
      </DialogTitle>

      <DialogActions>
        <Button autoFocus onClick={()=>{
          onClose()}
        } color="primary">
          No
        </Button>
        <Button onClick={() => {
          deleteSystem(systemId).then(onClose)
        }
        } color="primary">
          Yes
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
