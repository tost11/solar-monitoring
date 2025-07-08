import {Alert, Box, Button, IconButton, Input, InputAdornment, Modal} from "@mui/material";
import React, {useEffect, useState} from "react";
import {Login} from "../context/UserContext";
import {getRegistrationInfo, postRegister} from "../api/UserAPIFunctions";
import {Visibility, VisibilityOff} from "@material-ui/icons";
import ReplayIcon from '@material-ui/icons/Replay';
import {useTranslation} from "react-i18next";

interface RegisterProps {
  setLogin: (login: Login) => void;
  onClose: () => void;
  open: boolean;
}

export default function RegistrationView({setLogin, onClose, open}: RegisterProps) {

  const { t } = useTranslation()

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
  },[name,password,confirmPassword,mail,captchaText])

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
        return t("views.registration.invalid_mail")
    }
    if(name){
      if(!name.match("^[a-zA-Z0-9äöüÄÖÜßé]+(\\s+[a-zA-Z0-9äöüÄÖÜßé]+)*$")){
        return t("views.registration.invalid_username_1")
      }
      if(name.length < 4){
        return t("views.registration.invalid_username_2")
      }
    }
    if(password){
      if(!password.match(
        /^(?=.*[A-ZÄÜÖ])(?=.*[a-zäüöß])(?=.*\d)(?=.*[@$%*#?!&])[A-ZÄÖÜa-zäöüß\d@$!%*#?&]{10,64}$/
      )){
        return t("views.registration.invalid_password_1")
      }
    }
    if (confirmPassword !== password) {
      return t("views.registration.invalid_password_2")
    }
    if(captchaText && captchaText.length != 5){
      return t("views.registration.invalid_captcha")
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
      <div>{t("common.mail")}: <Input className="default-margin" type="text" placeholder="test@example.com" value={mail}
             onChange={event => setMail(event.target.value)}/>
      </div>
      <div>{t("common.username")}:<Input className="default-margin" type="text" placeholder="AwesomeUser123" value={name}
             onChange={event => setName(event.target.value)}/>
      </div>
      <div>{t("common.password")}: <Input className="default-margin" type={showPassword ? 'text' : 'password'} value={password}
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
      </div>
      <div>{t("views.registration.passowrd_again")}:
        <Input className="default-margin" type={showPassword ? 'text' : 'password'} value={confirmPassword}
          onChange={event => setConfirmPassword(event.target.value)} endAdornment={
           <InputAdornment position="end">
             <IconButton
               onClick={handleClickShowPassword}

             >
               {showPassword ? <VisibilityOff /> : <Visibility />}
             </IconButton>
           </InputAdornment>
          }
         />
      </div>

      {!captchaImage?
        <div>Capchar is Loading...</div>:
        <div style={{display:"flex"}}>
          <img src={"data:image/png;base64,"+captchaImage} alt="captchar" />
          <IconButton>
            <ReplayIcon style={{padding: "2px", cursor: "pointer"}} onClick={e=>reloadCaptcha()} />
          </IconButton>
          <Input className="default-margin" type="text" value={captchaText}
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
