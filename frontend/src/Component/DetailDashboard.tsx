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
import {getGraphColourByIndex} from "./utils/GraphUtils";

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

      //TODO check if old data cann be removed because it out time scope

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
        const treeMinutesAgo = moment().subtract(3, 'minutes')
        if(treeMinutesAgo.isBefore(moment(timeRange.time.end))) {
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

  const addUtcOffsetToTime = (date:Date,add:boolean)=>{
    console.log(date)
    // @ts-ignore
    var utcOffset = moment().tz(data.timezone).utcOffset();
    utcOffset -= moment(date).utcOffset();
    if(add) {
      return moment(date).add(utcOffset, "minutes").toDate()
    }else{
      return moment(date).subtract(utcOffset, "minutes").toDate()
    }
  }

  const internUpdateTimeRangeFromUserInput = (timeRange:TimeAndDuration) => {
    timeRange.start = addUtcOffsetToTime(timeRange.start,false)
    timeRange.end = addUtcOffsetToTime(timeRange.end,false)
    internUpdateTimeRange({fromInterval:false,time:timeRange})
  }

  const timeZoneTimeRangeFix = (timeRange:TimeAndDuration) => {
    var t = {...timeRange}
    t.start = addUtcOffsetToTime(timeRange.start,true)
    t.end = addUtcOffsetToTime(timeRange.end,true)
    return t
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

  const getColoursOfSelectedDevices = () => {
    if(!graphData || !graphData.deviceIds || graphData.deviceIds.length <= 0){
      return [getGraphColourByIndex(0)]
    }
    let res = []
    if(showCombined){
      res.push(getGraphColourByIndex(0))
    }
    for (let i = 0; i < graphData.deviceIds.length; i++) {
      if(checkDevices.has(graphData.deviceIds[i])){
        res.push(getGraphColourByIndex(i+1))
      }
    }
    return res;
  }

  return <div>
    {data && graphData ? <div style={{display:"flex", justifyContent:"center"}}>
      <div style={{display:"flex",flexDirection:"column"}}>
        <h3>{data.name}</h3>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            <div style={{margin:"10px"}}>
              Timezone: {data.timezone}
            </div>
          </div>
          <TimeAndDateSelector maxDate={addUtcOffsetToTime(new Date(),true)} onChange={internUpdateTimeRangeFromUserInput} timeRange={timeZoneTimeRangeFix(timeRange.time)} timeRanges={durations}/>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            Update: {graphData.timer != undefined ? "on":"off"}
          </div>
        </div>
        {graphData?.deviceIds && graphData?.deviceIds.length > 0 && <div className="defaultFlex">
          <div className="marginAuto">
            Possible Devices:
          </div>
          <FormControlLabel
              label={<div style={{color:getGraphColourByIndex(0)}}>Combined</div>}
              control={<Checkbox
                  checked={showCombined}
                  onChange={()=>setShowCombined(!showCombined)}
                  inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          {graphData.deviceIds.map((k,i)=><FormControlLabel
            key={i}
            label={<div style={{color:getGraphColourByIndex(i+1)}}>{"Device "+k}</div>}
            control={<Checkbox
                checked={checkDevices.has(k)}
                onChange={()=>changeDeviceSelection(k)}
                inputProps={{ 'aria-label': 'controlled' }}
            />}
          />)}
        </div>}
        <div>
          {data.type==="SELFMADE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion timezone={data.timezone} isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}

          {data.type==="SELFMADE_CONSUMPTION"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion timezone={data.timezone} isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion timezone={data.timezone} timeRange={timeRange.time} graphData={graphData} inverter={true} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_INVERTER"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion timezone={data.timezone} isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion timezone={data.timezone} inverterVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData} inverter={true} device={false}/>
            <StatisticsAccordion  systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SELFMADE_DEVICE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <BatteryAccordion timezone={data.timezone} isBatteryPercentage={data.isBatteryPercentage} minBatteryVoltage={minBV} maxBatteryVoltage={maxBV} timeRange={timeRange.time} graphData={graphData}/>
            <ConsumptionAccordion timezone={data.timezone} inverterVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData} inverter={false} device={true}/>
            <StatisticsAccordion systemInfo={data} consumption={true}/>
          </div>}
          {data.type==="SIMPLE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
          {data.type==="VERY_SIMPLE"&&<div className={"detailDashboard"}>
            <SolarPanelAccordion timezone={data.timezone} onlyWatt={true} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
          {data.type==="GRID"&&<div className={"detailDashboard"}>
            <GridInputAccordion timezone={data.timezone} deviceColours={getColoursOfSelectedDevices()} showCombined={showCombined} deviceIds={checkDevices} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <GridOutputAccordion timezone={data.timezone} deviceColours={getColoursOfSelectedDevices()} showCombined={showCombined} deviceIds={checkDevices} gridVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
        </div>
      </div>
    </div>:<CircularProgress/>}
  </div>
}

