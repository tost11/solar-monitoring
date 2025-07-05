import {Alert, Box, Button, IconButton, Input, InputAdornment, Modal} from "@mui/material";
import React, {useEffect, useState} from "react";
import {Login} from "../context/UserContext";
import {getRegistrationInfo, postRegister} from "../api/UserAPIFunctions";
import {Visibility, VisibilityOff} from "@material-ui/icons";
import ReplayIcon from '@material-ui/icons/Replay';

interface RegisterProps {
  setLogin: (login: Login) => void;
  onClose: () => void;
  open: boolean;
}

export default function RegistrationView({setLogin, onClose, open}: RegisterProps) {
  const [name, setName] = useState<string>();
  const [error, setError] = useState<string>();
  const [password, setPassword] = useState<string>();
  const [confirmPassword, setConfirmPassword] = useState<string>();
  const [showPassword, setShowPassword] = useState(false);
  const [captchaImage,setCaptchaImage] = useState<string>();
  const [captchaText,setCaptchaText] = useState("");
  const [mail ,setMail] = useState<string>();

  useEffect(()=>{
    setError(areRegisterConditionsFullFiled())
  },[name,password,confirmPassword,mail])

  const reloadCaptcha = ()=>{
    getRegistrationInfo().then(e => {
      setCaptchaImage(e.captcha);
    })
    setCaptchaText("");
  }

  useEffect(()=>{
    if(open) {
      reloadCaptcha();
    }
  },[open])


  const closeModal = () => {
    onClose()
    setName(null)
    setConfirmPassword(null)
    setPassword(null)
    setShowPassword(false)
    setMail(null)
  }
  const handleClickShowPassword= ()=>{
    setShowPassword(!showPassword)
  }

  const areRegisterConditionsFullFiled= ()=>{
    if(mail && !mail.toLowerCase().match(
        /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
      )){
        return "Not a valid Mail"
    }
    if(name){
      if(!name.match("^[a-zA-Z0-9äöüÄÖÜßé]+(\\s+[a-zA-Z0-9äöüÄÖÜßé]+)*$")){
        return "Name Contains illegal character"
      }
      if(name.length < 4){
        return "Name Must Contains 4 Characters"
      }
    }
    if(password){
      if(password.length < 8){
        return "Password Must Contains 8 Characters"
      }
    }
    if (confirmPassword !== password) {
      return "Password not equals"
    }
    if(captchaText && captchaText.length <= 0){
      return "Captcha not filled out"
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
      <Input className="default-margin" type="text" name="mail" placeholder="Mail-Adress" value={mail}
             onChange={event => setMail(event.target.value)}/>
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

      {!captchaImage?
        <div>Capchar is Loading...</div>:
        <div style={{display:"flex"}}>
          <img src={"data:image/png;base64,"+captchaImage} alt="captchar" />
          <IconButton>
            <ReplayIcon style={{padding: "2px", cursor: "pointer"}} onClick={e=>reloadCaptcha()} />
          </IconButton>
          <Input className="default-margin" type="text" name="captchaText" value={captchaText}
                 onChange={event => setCaptchaText(event.target.value)}
          />
        </div>
      }


      <Button variant="outlined" onClick={() => {
          postRegister(mail,name, password,captchaImage,captchaText).then(() => {
            closeModal()
          })
        }
      } disabled={error!==null || name === null || password === null || confirmPassword === null}>Register</Button>

    </Box>
  </Modal>
}
