import React, {useContext, useState} from "react";
import {Button, Input} from '@mui/material';
import {UserContext,Login,SolarSystem,SystemContext} from "../UserContext"
import { createSystem, getSystem } from "../api/SolarSystemAPI";

interface SystemPoops {
  setSystem: (system: SolarSystem) => void;
}

export default function CreateNewSystemComponent({setSystem}: SystemPoops) {
  const system = useContext(SystemContext);
  const [name,setName]=useState("");
  const doLogin = getSystem();
  doLogin({name}).then(setSystem)

  return <div>





    {system && <div>
      :
      <div><p>{system.token}</p>
      </div>
    </div>}
  </div>
}
