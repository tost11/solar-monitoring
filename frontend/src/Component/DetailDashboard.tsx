import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {CircularProgress} from "@mui/material";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import StatisticsAccordion from "./Accordions/StatisticsAccordion"
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";
import RefreshTimeSelector from "./RefreshTimeSelector";


export default function DetailDashboardComponent() {
  const initialState = {
    name:"",
    buildingDate:0,
    creationDate:0,
    type:"",
    id:0,
    isBatteryPercentage:false,
    inverterVoltage:0,
    batteryVoltage:0,
    maxSolarVoltage:0,

  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)
  const [refreshTime,setRefreshTime] = useState("1m")

  const params = useParams()
  const dashboardPath = "/grafana/d-solo/dashboard-" + params.id+"/dashboard-" + params.id;

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

      <div><RefreshTimeSelector setRefreshTime={(r)=>{setRefreshTime(r)}} refreshTime={refreshTime}/></div>
      {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion refresh={refreshTime} dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
      </div>}

      {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <ConsumptionAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
      </div>}
      {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion dashboardPath={dashboardPath} systemInfo={data}/>
        {/*consumption inverter*/}
      </div>}
      {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <ConsumptionAccordion refresh={refreshTime}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
        {/*consumption inverter*/}
        {/*consumption overall*/}
      </div>}





    </div>:<CircularProgress/>}

  </div>
}
