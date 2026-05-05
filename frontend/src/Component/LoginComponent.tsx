import React, {useContext, useState} from "react";
import {Box, Button, Input, Modal, Typography} from '@mui/material';
import {Login,UserContext} from "../context/UserContext"
import {postLogin, postPasswordResetRequest} from "../api/UserAPIFunctions";
import {useTranslation} from "react-i18next";
import {getRegistrationInfo} from "../api/UserAPIFunctions";
import RefreshIcon from '@mui/icons-material/Refresh';

interface LoginProps {
  setLogin: (login: Login) => void;
  onClose:()=>void;
  open:boolean;
}

export default function LoginComponent({setLogin,onClose,open}: LoginProps) {

  const { t } = useTranslation()

  const [name, setName] = useState("")
  const [password, setPassword] = useState("")
  const [showPasswordReset, setShowPasswordReset] = useState(false)
  const [resetEmail, setResetEmail] = useState("")
  const [captchaImage, setCaptchaImage] = useState("")
  const [captchaText, setCaptchaText] = useState("")
  const [resetSuccess, setResetSuccess] = useState(false)
  const [resetError, setResetError] = useState("")

  const closeModal = () => {
    onClose()
    setName("")
    setPassword("")
    setShowPasswordReset(false)
    setResetEmail("")
    setCaptchaImage("")
    setCaptchaText("")
    setResetSuccess(false)
    setResetError("")
  }

  const areLoginConditionsFullfiled = () => {
    return name && password && name.length!==0 && password.length!==0
  }

  const loadCaptcha = () => {
    getRegistrationInfo().then(response => {
      setCaptchaImage(response.captcha)
    })
  }

  const handlePasswordResetClick = () => {
    setShowPasswordReset(true)
    loadCaptcha()
  }

  const handleBackToLogin = () => {
    setShowPasswordReset(false)
    setResetEmail("")
    setCaptchaImage("")
    setCaptchaText("")
    setResetSuccess(false)
    setResetError("")
  }

  const handlePasswordResetSubmit = () => {
    setResetError("")
    postPasswordResetRequest({
      usernameOrEmail: resetEmail,
      captchaImage: captchaImage,
      captchaText: captchaText
    }).then(() => {
      setResetSuccess(true)
    }).catch(err => {
      setResetError(err.response?.data?.message || "Failed to request password reset")
    })
  }

  const areResetConditionsFullfiled = () => {
    return resetEmail && captchaText && resetEmail.length > 0 && captchaText.length > 0
  }

  return <Modal
    open={open}
    onClose={closeModal}
    aria-labelledby="modal-modal-title"
    aria-describedby="modal-modal-description"
  >

    <Box sx={{maxWidth:"90vw"}} className={"Modal"} >
      {!showPasswordReset ? (
        <>
          <Input className="Input" type="text" name="Loginname" value={name}
               onChange={(event)=> {
                 setName(event.target.value)
               }} placeholder={t("components.session.name-or-mail")}/>
          <Input className="Input" type="password" name="Loginpassword" value={password}
               onChange={(event) => {
                 setPassword(event.target.value)
               }} placeholder={t("common.password")}/>
          <Button variant="outlined" onClick={() => {
            postLogin(name, password).then((response) => {
              setLogin(response)
              closeModal()
            })
          }} disabled={!areLoginConditionsFullfiled()}>{t("components.session.login")}</Button>

          <Button variant="text" onClick={handlePasswordResetClick} style={{marginTop: '10px'}}>
            {t("components.session.forgot-password")}
          </Button>
        </>
      ) : (
        <>
          {!resetSuccess ? (
            <>
              <Typography variant="h6">{t("components.session.reset-password")}</Typography>
              <Input className="Input" type="text" name="ResetEmail" value={resetEmail}
                   onChange={(event)=> {
                     setResetEmail(event.target.value)
                   }} placeholder={t("components.session.username-or-email")}/>

              {captchaImage && (
                <>
                  <Box style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                    <img src={`data:image/png;base64,${captchaImage}`} alt="CAPTCHA"/>
                    <Button onClick={loadCaptcha} style={{minWidth: '40px', padding: '5px'}}>
                      <RefreshIcon />
                    </Button>
                  </Box>
                  <Input className="Input" type="text" name="CaptchaText" value={captchaText}
                       onChange={(event)=> {
                         setCaptchaText(event.target.value)
                       }} placeholder={t("components.session.captcha")} maxLength={5}/>
                </>
              )}

              {resetError && <Typography color="error">{resetError}</Typography>}

              <Button variant="outlined" onClick={handlePasswordResetSubmit}
                      disabled={!areResetConditionsFullfiled()}>
                {t("components.session.send-reset-link")}
              </Button>

              <Button variant="text" onClick={handleBackToLogin} style={{marginTop: '10px'}}>
                {t("components.session.back-to-login")}
              </Button>
            </>
          ) : (
            <>
              <Typography variant="h6">{t("components.session.reset-link-sent")}</Typography>
              <Typography>{t("components.session.check-email")}</Typography>
              <Button variant="outlined" onClick={closeModal} style={{marginTop: '20px'}}>
                {t("common.close")}
              </Button>
            </>
          )}
        </>
      )}
    </Box>

  </Modal>
}
