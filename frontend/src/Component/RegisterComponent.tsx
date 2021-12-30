import {Box, Button, Input, Modal} from "@mui/material";
import React, {useContext, useState} from "react";
import useLoginState from "../useLoginState";
import {Login, UserContext} from "../context/UserContext";
import {postRegister} from "../api/UserAPIFunctions";

interface RegisterProps {
  setLogin: (login: Login) => void;
  onClose: () => void;
  open: boolean;
}


export default function RegisterComponent({setLogin, onClose, open}: RegisterProps) {
  const login = useContext(UserContext);
  const isLogin = useLoginState()
  const [name, setName] = useState("")
  const [error, setError] = useState("")
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")

  const closeModal = () => {
    onClose()
    setName("")
    setPassword("")
  }
  const areRegisterConditionsFullfiled = () => {

    return name && password && confirmPassword && name.length !== 0 && password.length >= 8 && confirmPassword === password;
  }
  return <Modal
      open={open}
      onClose={closeModal}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
  >

    <Box className={"Modal"}>
      <Input className="default-margin" type="text" name="RegisterName" placeholder="RegisterName" value={name}
             onChange={event => setName(event.target.value)}/>
      <Input className="default-margin" type="password" name="RegisterPassword" placeholder="Password" value={password}
             onChange={event => setPassword(event.target.value)}/>
      <Input className="default-margin" type="password" name="ConfirmPassword" placeholder="ConfirmPassword"
             value={confirmPassword} onChange={event => setConfirmPassword(event.target.value)}/>


      <Button variant="outlined" onClick={() => {
        postRegister(name, password).then((response) => {
          setLogin(response)
          closeModal()
        })
      }
      } disabled={!areRegisterConditionsFullfiled()}>Login</Button>

    </Box>

  </Modal>
}
