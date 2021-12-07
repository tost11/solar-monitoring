import React from "react";
import {postLogin,Login} from "./LoginAPI";
import { Input,Button } from '@mui/material';
import {useState } from "react";
import { createContext } from 'react';
import {useContext } from "react";
import {UserContext} from "./UserContext"

interface LoginProps {
  setLogin:(login:Login)=>void;
}
export default function LoginComponent({setLogin}:LoginProps) {
  const login = useContext(UserContext);
  const [name,setName]=useState("")
  const [password,setPassword]=useState("")
  const Context = createContext('Default Value');



  return<div>
    <Input className="default-margin" type="text" name="Loginname" value={name} onChange={event=>setName(event.target.value)}/>
    <Input className="default-margin" type="password" name="Loginpassword"  value={password} onChange={event=>setPassword(event.target.value)}/>



    <Button variant="outlined" onClick={()=>{
      postLogin(name,password).then(setLogin)}
    }>Login</Button>

    {login && <div>
      Rendering system:
      <div><p>{login.name}</p>
      </div>
    </div>}
  </div>
}
