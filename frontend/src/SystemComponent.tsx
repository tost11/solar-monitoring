import React from "react";
import {createSystem,getSystem, SolarSystem} from "./SolarSystemAPI";
import Button from '@mui/material/Button';
import { useState } from "react";



export default function SystemComponent() {

  const [system,setSystem] = useState<null|SolarSystem>(null)

  return<div>
    <Button onClick={()=>{
      getSystem().then(setSystem)}
    }>load system</Button>

    {system && <div>
      Rendering system:
      <div>{system.name}
      </div>
    </div>}
  </div>
}
