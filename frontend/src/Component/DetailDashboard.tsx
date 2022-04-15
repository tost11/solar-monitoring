import React, {useEffect, useState} from "react";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useLocation, useNavigate, useParams, useSearchParams} from "react-router-dom";
import SolarPanelAccordion from "./Accordions/SolarPanelAccordion";
import BatteryAccordion from "./Accordions/BatteryAccordion";
import StatisticsAccordion from "./Accordions/StatisticsAccordion"
import ConsumptionAccordion from "./Accordions/ConsumptionAccordion";
import {fetchLastFiveMinutes, getAllGraphData} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../context/time/TimeAndDateSelector";
import moment from "moment";
import GridInputAccordion from "./Accordions/GridInputAccordion";
import GridOutputAccordion from "./Accordions/GridOutputAccordion";
import {Checkbox, CircularProgress, FormControlLabel} from "@mui/material";

export interface GraphDataObject{
  data:[]
  timer?:any,
  deviceIds?: number[]
}

export default function DetailDashboardComponent(){

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

  const [data, setData] = useState<SolarSystemDTO>()
  const [graphData,setGraphData]=useState<GraphDataObject>()
  const [timeRange,setTimeRange] = useState({fromInterval:false,time:generateTimeDuration(initDuration,initDate)})
  const [minBV,setMinBV] = useState<number>()
  const [maxBV,setMaxBV] = useState<number>()
  const [checkDevices,setCheckDevices] = useState(new Set<number>())
  const [showCombined,setShowCombined] = useState(true)

  const navigate = useNavigate();
  const location = useLocation()
  
  const internUpdateTimeRange = (timeRange:any)=>{//TODO replace any
      navigate({
        pathname: location.pathname,
        search: "?duration="+timeRange.time.durationString+"&date="+timeRange.time.end.getTime(),
      },{replace:true});
    setTimeRange(timeRange)
  }

  const updateGraphData = (systemId:number) => {
    if(!data){
      return
    }
    fetchLastFiveMinutes(systemId,data.type,timeRange.time.duration).then(res=>{
      // @ts-ignore
      let newData = []
      if(res.data.length > 0) {
        graphData?.data.forEach(d => {
          // @ts-ignore
          if (d.time > timeRange.time.start.getTime() && d.time < res.data[0].time) {
            newData.push(d)
          }
        })
        res.data.forEach(d=>{
          newData.push(d)
        })
      }else{
        graphData?.data.forEach(d => {
            newData.push(d)
        })
      }
      // @ts-ignore
      let timer = setTimeout(()=>internUpdateTimeRange({fromInterval:true,time:generateTimeDuration(timeRange.time.durationString,new Date())}),1000 * 60)
      console.log("Start new timeout ",timer)

      //handle new deviceIds
      let newDevices = new Set<number>()
      graphData?.deviceIds?.forEach(d=>newDevices.add(d))
      res.deviceIds?.forEach(d=>{
        if(newDevices.has(d) === false){
          newDevices.add(d)
        }
      })

      // @ts-ignore
      setGraphData({data:newData,deviceIds: newDevices.length===0?undefined:Array.from(newDevices),timer:timer})
    })
  }

  const checkGraphData = (res:SolarSystemDTO) => {
    if(timeRange.fromInterval){
      updateGraphData(res.id)
    }else{
      if(graphData && graphData.timer){
        console.log("clear timeout ",graphData.timer)
        clearTimeout(graphData.timer)
      }
      // @ts-ignore
      getAllGraphData(res.id,res.type,timeRange.time.start.getTime(), timeRange.time.end.getTime()).then((r)=>{
        let timer = undefined;
        const twoMinutesAgo = moment().subtract(2, 'minutes')
        if(twoMinutesAgo.isBefore(moment(timeRange.time.end))) {
          timer = setTimeout(() => internUpdateTimeRange({
            fromInterval: true,
            time: generateTimeDuration(timeRange.time.durationString, new Date())
          }), 1000 * 60)
          console.log("Start new timeout ",timer)
        }
        setGraphData({data:r.data,deviceIds:r.deviceIds,timer:timer})
      })
    }
  }

  useEffect(() => {

  if(data){
    checkGraphData(data);
    return
  }

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
      checkGraphData(res)
      setData(res)
  })}}, [timeRange])

  const internUpdateTimeRangeFromUserInput = (timeRange:TimeAndDuration) => {
    internUpdateTimeRange({fromInterval:false,time:timeRange})
  }

  const changeDeviceSelection = (id:number)=>{
    var newSelection = new Set<number>(checkDevices)
    if(newSelection.has(id)){
      newSelection.delete(id)
    }else{
      newSelection.add(id)
    }
    setCheckDevices(newSelection)
  }

  return <div>
    {data && graphData ? <div style={{display:"flex", justifyContent:"center"}}>
      <div style={{display:"flex",flexDirection:"column"}}>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"20px", marginLeft:"10px"}}>
            <h3>{data.name}</h3>
          </div>
          <TimeAndDateSelector maxDate={new Date()} onChange={internUpdateTimeRangeFromUserInput} timeRange={timeRange.time} timeRanges={durations}/>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            Update: {graphData.timer != undefined ? "on":"off"}
          </div>
        </div>
        {graphData?.deviceIds && graphData?.deviceIds.length > 0 && <div className="defaultFlex">
          <div className="marginAuto">
            Possible Devices:
          </div>
          <FormControlLabel
              label={"Combined"}
              control={<Checkbox
                  checked={showCombined}
                  onChange={()=>setShowCombined(!showCombined)}
                  inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          {graphData.deviceIds.map((k,i)=><FormControlLabel
            key={i}
            label={k==0?"Kombined":"Device "+k}
            control={<Checkbox
                checked={checkDevices.has(k)}
                onChange={()=>changeDeviceSelection(k)}
                inputProps={{ 'aria-label': 'controlled' }}
            />}
          />)}
        </div>}
        <div>
          {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}

          {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion timeRange={timeRange.time} graphData={graphData} inverter={true} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion inverterVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData} inverter={true} device={false}/>
            <StatisticsAccordion  systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion inverterVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData} inverter={false} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SIMPLE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
          {data.type==="VERY_SIMPLE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion onlyWatt={true} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
          {data.type==="GRID"&&<div className={"detailDashboard"}>
            <GridInputAccordion showCombined={showCombined} deviceIds={checkDevices} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <GridOutputAccordion showCombined={showCombined} deviceIds={checkDevices} gridVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
        </div>
      </div>
    </div>:<CircularProgress/>}
  </div>
}

