import React from "react";
import { useState } from "react";
import "./main.css"
import LoginComponent from "./Component/LoginComponent"
import {UserContext,SystemContext,Login,SolarSystem} from "./UserContext"
import SystemComponent from "./Component/SystemComponent"




export default function App() {
  const [login,setLogin]=useState<null|Login>(null);
  const [system,setSystem]=useState<null|SolarSystem>(null);
  return <div>
      <SystemComponent setSystem={setSystem}/>


  </div>
}
