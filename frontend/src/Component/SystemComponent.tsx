import React, {useEffect, useState} from "react";
import {getSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import SystemAccordion from "./Accordions/SystemAccordion";

export default function SystemComponent() {
  const [data, setData] = useState<SolarSystemListDTO[]>([])
  useEffect(() => {
    getSystems().then((res) => {
      console.log(res)
      // @ts-ignore
      setData(res)
    })
  }, [])

  return <div>

    {data.length>0&&
    data.map((e,i)=>
    <SystemAccordion key={i} id={e.id} name={e.name} type={e.type} />)
    }



  </div>
}

