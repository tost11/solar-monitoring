import {Alert, Box, Button, Input, Modal } from "@mui/material";
import React from "react";
import { useContext } from "react";
import { useEffect } from "react";
import { useState } from "react";
import ReactDOM from "react-dom";
import {postRegister} from "../api/RegisterAPI";
import useLoginState from "../useLoginState";
import { UserContext, Login} from "../UserContext";

interface RegisterProps {
  setLogin: (login: Login) => void;
  onClose:()=>void;
  open:boolean;
}


export default function RegisterComponent({setLogin,onClose,open}: RegisterProps) {
  const login = useContext(UserContext);
  const isLogin=useLoginState()
  const [name,setName]=useState("")
  const [error,setError]=useState("")
  const [password,setPassword]=useState("")
  const [confirmPassword,setConfirmPassword]=useState("")
  const [openError,setOpenError]= useState("")
  const doRegister = postRegister();

  const closeModal = () => {
    onClose()
    setName("")
    setPassword("")
    setOpenError("")
  }
  const areRegisterConditionsFullfiled = () => {

    return name && password &&confirmPassword && name.length !== 0 && password.length !== 8 && confirmPassword==password;
  }
return<Modal
    open={open}
    onClose={closeModal}
    aria-labelledby="modal-modal-title"
    aria-describedby="modal-modal-description"
  >

    <Box className={"Modal"} >

      {openError &&(
        <Alert className={"ErrorAlert"} severity="error" >{openError}</Alert>
      )
      }

      <Input className="default-margin" type="text" name="RegisterName" placeholder="RegisterName"  value={name} onChange={event=>setName(event.target.value)}/>
      <Input className="default-margin" type="password" name="RegisterPassword" placeholder="Password" value={password} onChange={event=>setPassword(event.target.value)}/>
      <Input className="default-margin" type="password" name="ConfirmPassword" placeholder="ConfirmPassword" value={confirmPassword} onChange={event=>setConfirmPassword(event.target.value)}/>


      <Button variant="outlined" onClick={() => {
        doRegister({name, password}).then((response)=>{
          setLogin(response)

          closeModal()
        }).catch((e:Response)=>{
          e.json().then((k)=>{
            console.log(k.error)
          })})}
      } disabled={!areRegisterConditionsFullfiled()}>Login</Button>

    </Box>

  </Modal>
}
