import {Alert, Box, Button, IconButton, Input, InputAdornment, Modal} from "@mui/material";
import React, {useContext, useEffect, useState} from "react";
import useLoginState from "../useLoginState";
import {Login,UserContext} from "../context/UserContext";
import {postRegister} from "../api/UserAPIFunctions";
import { Visibility, VisibilityOff } from "@material-ui/icons";

interface RegisterProps {
  setLogin: (login: Login) => void;
  onClose: () => void;
  open: boolean;
}


export default function RegisterComponent({setLogin, onClose, open}: RegisterProps) {
  const login = useContext(UserContext);
  const isLogin = useLoginState();
  const [name, setName] = useState<string|null>(null);
  const [error, setError] = useState<string|null>(null);
  const [password, setPassword] = useState<string|null>(null);
  const [confirmPassword, setConfirmPassword] = useState<string|null>(null);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(()=>{
    setError(areRegisterConditionsFullFiled())
  },[name,password,confirmPassword])

  const closeModal = () => {
    onClose()
    setName(null)
    setConfirmPassword(null)
    setPassword(null)
    setShowPassword(false)
  }
  const handleClickShowPassword= ()=>{
    setShowPassword(!showPassword)
  }

  const areRegisterConditionsFullFiled= ()=>{
    if(name!==null){
      if(name.length<4){
        return "Name Must Contains 4 Characters"
      }
    }
    if(password!==null){
      if(password.length<8){
        return "Password Must Contains 8 Characters"
      }
    }
    if (confirmPassword !== password) {
      return "Password not equals"
    }
    return null;
  }

  return <Modal
      open={open}
      onClose={closeModal}
      aria-labelledby="modal-modal-title"
      aria-describedby="modal-modal-description"
  >

    <Box className={"RegisterModal"}>
      {error && <Alert severity="error">{error}</Alert>
      }
      <Input className="default-margin" type="text" name="RegisterName" placeholder="RegisterName" value={name}
             onChange={event => setName(event.target.value)}/>
      <Input className="default-margin" type={showPassword ? 'text' : 'password'} name="RegisterPassword" placeholder="Password" value={password}
             onChange={event => setPassword(event.target.value)} endAdornment={
        <InputAdornment position="end">
          <IconButton
            aria-label="toggle password visibility"
            onClick={handleClickShowPassword}

          >
            {showPassword ? <VisibilityOff /> : <Visibility />}
          </IconButton>
        </InputAdornment>
      }/>
      <Input className="default-margin" type={showPassword ? 'text' : 'password'} name="ConfirmPassword" placeholder="ConfirmPassword" value={confirmPassword}
             onChange={event => setConfirmPassword(event.target.value)} endAdornment={
               <InputAdornment position="end">
                 <IconButton
                   aria-label="toggle password visibility"
                   onClick={handleClickShowPassword}

                 >
                   {showPassword ? <VisibilityOff /> : <Visibility />}
                 </IconButton>
               </InputAdornment>
             }
             />


      <Button variant="outlined" onClick={() => {
        postRegister(name, password).then((response) => {
          setLogin(response)
          closeModal()
        })
      }
      } disabled={error!==null || name === null || password === null || confirmPassword === null}>Register</Button>

    </Box>
  </Modal>
}
