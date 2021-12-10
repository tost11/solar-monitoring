import React, {useContext, useState} from "react";
import {Button, Input} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import {postLogin} from "../api/LoginAPI";

interface LoginProps {
  setLogin: (login: Login) => void;
}

export default function LoginComponent({setLogin}: LoginProps) {
  const login = useContext(UserContext);
  const [name, setName] = useState("")
  const [password, setPassword] = useState("")

  const doLogin = postLogin();

  return <div>
    <Input className="default-margin" type="text" name="Loginname" value={name}
           onChange={event => setName(event.target.value)}/>
    <Input className="default-margin" type="password" name="Loginpassword" value={password}
           onChange={event => setPassword(event.target.value)}/>


    <Button variant="outlined" onClick={() => {
      doLogin({name, password}).then(setLogin)}
    }>Login</Button>

    {login && <div>
      Rendering system:
      <div><p>{login.name}</p>
      </div>
    </div>}
  </div>
}
