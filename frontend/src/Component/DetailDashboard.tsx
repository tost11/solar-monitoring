import React, {useEffect, useState} from "react";
import {CircularProgress} from "@mui/material";
import {getSystem, SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {useParams, useSearchParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import StatisticsAccordion from "./Accordions/StatisticsAccordion"
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";
import {getAllGraphData} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration} from "../context/time/TimeAndDateSelector";

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

  const params = useParams()

  const [searchParams, setSearchParams] = useSearchParams();

  const durations = ["5m","10m","30m","1h","2h","4h","6h","12h","24h"]

  const durationPara = searchParams.get("duration")
  let initDuration = (durationPara && durations.includes(durationPara)) ? durationPara:"1h"
  let dateParam = searchParams.get("date")
  let initDate = new Date();
  if(dateParam){
    var d = new Date(parseInt(dateParam))
    if(!isNaN(d.getTime())){
      initDate = d
    }
  }

  const [data, setData] = useState<SolarSystemDashboardDTO>(initialState)
  const [graphData,setGraphData]=useState<GraphDataObject>()
  const [timeRange,setTimeRange] = useState(generateTimeDuration(initDuration,initDate))
  const [minBV,setMinBV] = useState<number>()
  const [maxBV,setMaxBV] = useState<number>()

  useEffect(() => {
   if(!isNaN(Number(params.id))){
    getSystem(""+params.id).then((res) => {
      setData(res)
      if(res.batteryVoltage){
        if(res.batteryVoltage<20){
          setMinBV(res.batteryVoltage-2)
          setMaxBV(res.batteryVoltage+2)
        }else if(res.batteryVoltage<40){
          setMinBV(res.batteryVoltage-4)
          setMaxBV(res.batteryVoltage+4)
        }else if(res.batteryVoltage<60){
          setMinBV(res.batteryVoltage-6)
          setMaxBV(res.batteryVoltage+6)
        }else if(res.batteryVoltage<80){
          setMinBV(res.batteryVoltage-8)
          setMaxBV(res.batteryVoltage+8)
        }
      }
      getAllGraphData(res.id,timeRange.start.getTime(), timeRange.end.getTime()).then((r)=>{
        setGraphData({data:r})
      })
  })}}, [timeRange])

  return <div>
    {graphData ? <div style={{display:"flex", justifyContent:"center"}}>
      <div style={{display:"flex",flexDirection:"column"}}>
        <div>
          <TimeAndDateSelector onChange={setTimeRange} initialDate={initDate} initialTimeRange={initDuration} timeRanges={durations}/>
        </div>
        <div>
          {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}

          {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange} graphData={graphData}/>
            <ConsumptionAccordion timeRange={timeRange} graphData={graphData} inverter={true} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange} graphData={graphData}/>
            <ConsumptionAccordion inverterVoltage={data.inverterVoltage} timeRange={timeRange} graphData={graphData} inverter={true} device={false}/>
            <StatisticsAccordion  systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange} graphData={graphData}/>
            <ConsumptionAccordion inverterVoltage={data.inverterVoltage} timeRange={timeRange} graphData={graphData} inverter={false} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
        </div>
      </div>
    </div>:<CircularProgress/>}
  </div>
}

