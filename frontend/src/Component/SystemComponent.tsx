import React, { useContext } from "react";
import {createSystem, getSystems} from "../api/SolarSystemAPI";
import Button from '@mui/material/Button';
import {useState} from "react";
import { UserContext, Login} from "../UserContext";

interface SystemProps {
  setLogin: (login:Login|null) => void;
}

export default function SystemComponent({setLogin}:SystemProps) {
  const login= useContext(UserContext)
  const [name,setName]=useState("")
  const doCreateSystem = createSystem();
  const doSystems = getSystems();
  const x = () => {
    if (login!==null){
      return <div>{login.name}</div>
    }
  }
  doSystems({}).then((response:any)=> {
    for(let i=0;response.length>i;i++){
      console.log(response[i])
    }
  })

  return<div>
      <div>
      </div>

    </div>

}
