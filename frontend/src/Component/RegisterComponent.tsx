import {Button, Input } from "@mui/material";
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
}


export default function RegisterComponent({setLogin}: RegisterProps) {
  const login = useContext(UserContext);
  const isLogin=useLoginState()
  const [name,setName]=useState("")
  const [error,setError]=useState("")
  const [password,setPassword]=useState("")
  const [confirmPassword,setConfirmPassword]=useState("")
  const doRegister = postRegister();

return<div>
  <Input className="default-margin" type="text" name="RegisterName" placeholder="RegisterName"  value={name} onChange={event=>setName(event.target.value)}/>
  <Input className="default-margin" type="password" name="RegisterPassword" placeholder="Password" value={password} onChange={event=>setPassword(event.target.value)}/>
  <Input className="default-margin" type="password" name="ConfirmPassword" placeholder="ConfirmPassword" value={confirmPassword} onChange={event=>setConfirmPassword(event.target.value)}/>



  <Button variant="outlined" onClick={()=>{
    doRegister({name,password,confirmPassword}).then(setLogin).catch((e:Response)=>{
      e.json().then((k)=>{
        console.log(k)
      }
      )})}
  }>Login</Button>

  {login && <div>
    Rendering system:
    <div><p>{login.name}</p>
    </div>
    <div><p>{error}</p>
    </div>
  </div>}
</div>
}
