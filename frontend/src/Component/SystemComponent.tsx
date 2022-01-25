import React, { useEffect, useState} from "react";
import {getSystems,SolarSystemListDTO} from "../api/SolarSystemAPI";
import SystemAccordion from "./SystemAccordion";







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
    data.map((e)=>
    <SystemAccordion key={e.id} id={e.id} name={e.name} type={e.type} />)
    }



  </div>
}

