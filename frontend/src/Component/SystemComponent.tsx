import React, {useContext, useEffect, useState} from "react";
import {getSystems,SolarSystemListDTO} from "../api/SolarSystemAPI";
import {Simulate} from "react-dom/test-utils";
import MyAccordion from "./MyAccordion";







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
    <MyAccordion id={e.id} name={e.name} type={e.type} />)
    }



  </div>
}

