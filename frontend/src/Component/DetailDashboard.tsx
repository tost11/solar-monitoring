import React, {useEffect, useState} from "react";
import {CircularProgress} from "@mui/material";
import {getSystem, SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import StatisticsAccordion from "./Accordions/StatisticsAccordion"
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";
import TimeSelector, {convertToDuration} from "./TimeSelector";
import {getAllGraphData} from "../api/GraphAPI";

export interface GraphDataObject{
  data:Object[]
}

export default function DetailDashboardComponent(){
  const initialState = {
    name: "",
    buildingDate: new Date(),
    creationDate: new Date(),
    type: "",
    id: 0,
  };
  const [data, setData] = useState<SolarSystemDashboardDTO>(initialState)
  const [graphData,setGraphData]=useState<GraphDataObject>()
  const [timeRange,setTimeRange] = useState("1h")


  const params = useParams()
  const dashboardPath = "/grafana/d-solo/dashboard-" + params.id + "/dashboard-" + params.id;

  useEffect(() => {
   if(!isNaN(Number(params.id))){
     console.log("a")
    getSystem(""+params.id).then((res) => {
      setData(res)
      getAllGraphData(res.id,convertToDuration(timeRange).start.getTime()).then((r)=>{
        setGraphData({data:r})
      })
  })}}, [timeRange])

  const time = "30s";
  return <div>
    {graphData ? <div style={{display:"flex",justifyContent:"center"}}>
      <TimeSelector setTime={setTimeRange} initialValue={timeRange} values={["5m","10m","30m","1h","2h","4h","6h","12h","24h"]}/>
      {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} graphData={graphData}/>
        <BatteryAccordion timeRange={timeRange} graphData={graphData}/>
        <StatisticsAccordion systemInfo={data} consumption={false}/>
      </div>}

      {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} graphData={graphData}/>
        <BatteryAccordion timeRange={timeRange} graphData={graphData}/>
        <ConsumptionAccordion timeRange={timeRange} graphData={graphData} inverter={true} device={true}/>
        <StatisticsAccordion systemInfo={data} consumption={true}/>
      </div>}
      {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} graphData={graphData}/>
        <BatteryAccordion timeRange={timeRange} graphData={graphData}/>
        <ConsumptionAccordion timeRange={timeRange} graphData={graphData} inverter={true} device={false}/>
        <StatisticsAccordion  systemInfo={data} consumption={true}/>
      </div>}
      {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
        <SolarPanelAccordion timeRange={timeRange} graphData={graphData}/>
        <BatteryAccordion timeRange={timeRange} graphData={graphData}/>
        <ConsumptionAccordion timeRange={timeRange} graphData={graphData} inverter={false} device={true}/>
        <StatisticsAccordion systemInfo={data} consumption={true}/>
      </div>}

    </div>:<CircularProgress/>}
  </div>
}

