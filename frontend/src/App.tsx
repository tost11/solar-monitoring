import React from "react";
import {createSystem,getSystem} from "./SolarSystemAPI";
import Button from '@mui/material/Button';
import { useState } from "react";
import SystemComponent from "./SystemComponent";
import "./main.css"
import LoginComponent from "./LoginComponent"
import {UserContext} from "./UserContext"
import {Login} from "./api/LoginAPI";



export default function App() {
  const [login,setLogin]=useState<null|Login>(null);

  return <div>
    <UserContext.Provider value={login}>
      <LoginComponent setLogin={setLogin}/>
    </UserContext.Provider>


  </div>
}
