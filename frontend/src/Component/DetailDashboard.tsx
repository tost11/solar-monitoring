import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {CircularProgress} from "@mui/material";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import DayAccordion from "./Accordions/DayAccordion";
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";


export default function DetailDashboardComponent() {
  const initialState = {
    name:"",
    creationDate:0,
    type:"",
    grafanaUid:"",
    token:"",
  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)


  const params = useParams()

  useEffect(() => {
   if(!isNaN(Number(params.id))){
    getSystem(""+params.id).then((res) => {
      setData(res)
    }).then(()=>
      setIsLoading(true))
  }}, [])
  const login = useContext(UserContext);
  const time = "30s";
  return <div>
    {isLoading ? <div style={{display:"flex",justifyContent:"center"}}>

      {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <DayAccordion name={data.name} grafanaUid={data.grafanaUid}/>
      </div>}

      {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <ConsumptionAccordion systemInfo={data}/>
        <DayAccordion name={data.name} grafanaUid={data.grafanaUid}/>


      </div>}
      {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <DayAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        {/*consumption inverter*/}
      </div>}
      {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <ConsumptionAccordion systemInfo={data}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <DayAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        {/*consumption inverter*/}
        {/*consumption overall*/}
      </div>}





    </div>:<CircularProgress/>}

  </div>
}
