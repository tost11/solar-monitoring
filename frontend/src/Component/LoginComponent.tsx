import React, {useContext, useState} from "react";
import {Box, Button, Input, Modal} from '@mui/material';
import {Login,UserContext} from "../context/UserContext"
import {postLogin} from "../api/UserAPIFunctions";

interface LoginProps {
  setLogin: (login: Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LoginProps) {
  const login = useContext(UserContext);
  const [name, setName] = useState("")
  const [password, setPassword] = useState("")
  //const doLogin = postLogin();

  const closeModal = () => {
    onClose()
    setName("")
    setPassword("")
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
      <Input className="Input" type="text" name="Loginname" value={name}
           onChange={(event)=> {
             setName(event.target.value)
           }} placeholder="Name"/>
      <Input className="Input" type="password" name="Loginpassword" value={password}
           onChange={(event) => {
             setPassword(event.target.value)
           }} placeholder="Password"/>


    <Button variant="outlined" onClick={() => {
      postLogin(name, password).then((response) => {
        setLogin(response)
        closeModal()
      })
    }} disabled={!areLoginConditionsFullfiled()}>Login</Button>

    </Box>

  </Modal>
}
