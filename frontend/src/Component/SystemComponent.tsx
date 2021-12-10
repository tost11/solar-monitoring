import React, { useContext } from "react";
import {createSystem, getSystem} from "../api/SolarSystemAPI";
import Button from '@mui/material/Button';
import {useState} from "react";
import { UserContext, SolarSystem,SystemContext} from "../UserContext";

interface SystemProps {
  setSystem: (system: SolarSystem) => void;
}

export default function SystemComponent({setSystem}:SystemProps) {
  const system = useContext(SystemContext);
  const [name,setName]=useState("")

  const doCreateSystem = createSystem();

  return <div>

    // <p> du stinkst </p>
    
    <Button variant="outlined" onClick={() => {
      doCreateSystem({name}).then(setSystem)
    }
    }>Ertstell user</Button>

    {system && <div>
      Rendering system:
      <div>{system.name}
      </div>
    </div>}
  </div>
}
