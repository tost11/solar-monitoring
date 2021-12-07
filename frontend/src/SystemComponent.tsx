import React from "react";
import {createSystem,getSystem, SolarSystem} from "./SolarSystemAPI";
import Button from '@mui/material/Button';
import { useState } from "react";



export default function SystemComponent() {

  const [systems,setSystems] = useState<null|SolarSystem[]>(null)

  return<div>
    <Button onClick={()=>{
      getSystem(setSystems)}
    }>load systems</Button>

    {systems && <div>
      Rendering systems:
      {systems.forEach((s:SolarSystem)=>{
        return <div>
          {s.name}
        </div>
      })}
    </div>}
  </div>
}
