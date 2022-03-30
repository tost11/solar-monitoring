import React, {useEffect, useState} from "react";
import {getSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import SystemAccordion from "./Accordions/SystemAccordion";

export default function SystemComponent() {
  const [data, setData] = useState<SolarSystemListDTO[]>([])


  const reloadSystems = () => {
    getSystems().then((res) => {
      setData(res)
    })
  }
  useEffect(()=>reloadSystems(),[])
  return <div>

    {data.length>0&&
    data.map((e,i)=>
      <SystemAccordion key={i} system={e} reloadSystems={reloadSystems} />)
    }

  </div>
}

