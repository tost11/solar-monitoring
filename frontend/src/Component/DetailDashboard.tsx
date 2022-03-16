import React, {useEffect, useState} from "react";
import {CircularProgress} from "@mui/material";
import {getSystem, SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import StatisticsAccordion from "./Accordions/StatisticsAccordion"
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";
import TimeSelector from "./TimeSelector";


export default function DetailDashboardComponent() {
  const initialState = {
    name:"",
    buildingDate:new Date(),
    creationDate:new Date(),
    type:"",
    id:0,
  };
  const [data, setData] = useState<SolarSystemDashboardDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)
  //const [refreshTime,setRefreshTime] = useState("1m")
  //const [refreshTime,setRefreshTime] = useState("1m")
  const [timeRange,setTimeRange] = useState("1h")

  const params = useParams()
  const dashboardPath = "/grafana/d-solo/dashboard-" + params.id+"/dashboard-" + params.id;

  useEffect(() => {
   if(!isNaN(Number(params.id))){
    getSystem(""+params.id).then((res) => {
      setData(res)
    }).then(()=>
      setIsLoading(true))
  }}, [])
  const time = "30s";
  return <div>
    {isLoading ? <div style={{display:"flex",justifyContent:"center"}}>

      <div><TimeSelector setTime={setTimeRange} initialValue={timeRange} values={["5m","10m","30m","1h","2h","4h","6h","12h","24h"]}/></div>
      {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} refresh={time} dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
      </div>}

      {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <ConsumptionAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
      </div>}
      {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion dashboardPath={dashboardPath} systemInfo={data}/>
        {/*consumption inverter*/}
      </div>}
      {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <BatteryAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <ConsumptionAccordion timeRange={timeRange} refresh={time}  dashboardPath={dashboardPath} systemInfo={data}/>
        <StatisticsAccordion  dashboardPath={dashboardPath} systemInfo={data}/>
        {/*consumption inverter*/}
        {/*consumption overall*/}
      </div>}





    </div>:<CircularProgress/>}

  </div>
}
