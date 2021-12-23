import React, {useContext, useState} from "react";
import {Alert, Box, Button, Input, Modal} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import {postLogin} from "../api/LoginAPI";

interface LoginProps {
  setLogin: (login: Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LoginProps) {
  const login = useContext(UserContext);
  const [name, setName] = useState("")
  const [password, setPassword] = useState("")
  const [openError,setOpenError]= useState("")
  const doLogin = postLogin();

  const closeModal = () => {
    onClose()
    setName("")
    setPassword("")
    setOpenError("")
  }
  const areLoginConditionsFullfiled = () => {
    return name && password && name.length!==0 && password.length!==0
  }

  return <Modal
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

      <Input className="Input" type="text" name="Loginname" value={name}
           onChange={(event)=> {
             setName(event.target.value)
           }} placeholder="Name"/>
      <Input className="Input" type="password" name="Loginpassword" value={password}
           onChange={(event) => {
             setPassword(event.target.value)
           }} placeholder="Password"/>


    <Button variant="outlined" onClick={() => {
      doLogin({name, password}).then((response)=>{
        setLogin(response)

        closeModal()
      }).catch((e:Response)=>{
        e.json().then((k)=>{
          setOpenError(k.error)
                  })})}
    } disabled={!areLoginConditionsFullfiled()}>Login</Button>

    </Box>

  </Modal>
}
