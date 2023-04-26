import React, {useEffect, useRef, useState} from "react";
import {BooleanStatus, getSystem, SolarSystemDTO, SolarSystemType} from "../api/SolarSystemAPI";
import {useLocation, useNavigate, useParams, useSearchParams} from "react-router-dom";
import BatteryAccordion from "../Component/Accordions/BatteryAccordion";
import StatisticsAccordion from "../Component/Accordions/StatisticsAccordion"
import {fetchLastFiveMinutes, getAllGraphData, GraphDataDTO, GraphDataObject} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../Component/time/TimeAndDateSelector";
import InputAccordion from "../Component/Accordions/InputAccordion";
import OutputAccordion from "../Component/Accordions/OutputAccordion";
import {Accordion, AccordionDetails, AccordionSummary, Button, CircularProgress, Typography} from "@mui/material";
import {getGraphColourByIndex} from "../Component/utils/GraphUtils";
import CheckBoxComponentFilters from "../Component/CheckBoxComponentFilters";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import SetStatusList from "../Component/SetStatusList";
import ContinuousUpdateWrapper from "../Component/ContinuousUpdateWrapper";

export default function DetailDashboardComponent(){

  const params = useParams()

  const [searchParams, setSearchParams] = useSearchParams();

  const durations = ["5m","10m","30m","1h","2h","4h","6h","12h","24h"]

  const durationPara = searchParams.get("duration")
  let initDuration = (durationPara && durations.includes(durationPara)) ? durationPara:"1h"
  let dateParam = searchParams.get("date")
  let initDate = null;
  if(dateParam){
    var d = new Date(parseInt(dateParam))
    if(!isNaN(d.getTime())){
      initDate = d
    }
  }

  const [graphData, setGraphData] = useState<GraphDataObject>()
  const refGraphData = useRef<GraphDataObject>()
  const [data, setData] = useState<SolarSystemDTO>()
  const refTimeRange = useRef({autoUpdate:true,time:generateTimeDuration(initDuration,initDate?initDate:new Date())})
  const [timeRange,setTimeRange] = useState(refTimeRange.current)
  const [minBV,setMinBV] = useState<number>()
  const [maxBV,setMaxBV] = useState<number>()
  const [checkedDeviceIds,setCheckedDeviceIds] = useState(new Set<string>())
  const [checkedInputDCIds,setCheckedInputDCIds] = useState(new Set<string>())
  const [checkedInputACIds,setCheckedInputACIds] = useState(new Set<string>())
  const [checkedOutputDCIds,setCheckedOutputDCIds] = useState(new Set<string>())
  const [checkedOutputACIds,setCheckedOutputACIds] = useState(new Set<string>())
  const [colorsByName,setColorsByName] = useState(new Map<string,string>())
  const [checkedBatteryIds,setCheckedBatteryIds] = useState(new Set<string>())
  const [showCombined,setShowCombined] = useState(true)
  const [booleanStatus, setBooleanStatus] = useState<BooleanStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(false)

  const navigate = useNavigate()
  const location = useLocation()

  const internUpdateTimeRange = async (newTimeRange: TimeAndDuration, autoUpdate: boolean, forceReload?: boolean) => {//TODO replace any
    navigate({
      pathname: location.pathname,
      search: "?duration=" + newTimeRange.durationString + (!autoUpdate ? "&date=" + newTimeRange.end.getTime() : ""),
    }, {replace: true})
    let fullFetch = forceReload || autoUpdate == false || (refTimeRange.current.autoUpdate == false && autoUpdate == true) || newTimeRange.duration != refTimeRange.current.time.duration
    refTimeRange.current = {autoUpdate: autoUpdate, time: newTimeRange}
    setTimeRange(refTimeRange.current)
    if (fullFetch) {
      // @ts-ignore
      return await fetchFullGraphData(data.id,newTimeRange)
    } else {
      // @ts-ignore
      return await continuousUpdateDataCallback(data.id,newTimeRange)
    }
  }

  const updateColors = (data:GraphDataObject)=>{
    //let colors = {main:[],devices:[],inputs:[],outputs:[],batteries:[]}
    let colors = new Map<string,string>()

    let i = 1;
    //colors.main.push(getGraphColourByIndex(i++))
    for (let devicesKey in data.devices) {
      //colors.devices.push(getGraphColourByIndex(i++))
      colors.set("d-"+devicesKey,getGraphColourByIndex(i++))
    }
    for (let devicesKey in data.devices) {
      for (let id of data.devices[devicesKey].inputDCIds) {
        //colors.inputs.push(getGraphColourByIndex(i++))
        colors.set("i-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].inputACIds) {
        //colors.inputs.push(getGraphColourByIndex(i++))
        colors.set("j-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].outputDCIds) {
        //colors.outputs.push(getGraphColourByIndex(i++))
        colors.set("o-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].outputACIds) {
        //colors.outputs.push(getGraphColourByIndex(i++))
        colors.set("c-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].batteryIds) {
        //colors.batteries.push(getGraphColourByIndex(i++))
        colors.set("b-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
    }
    setColorsByName(colors)
  }

  const continuousUpdateDataCallback = async (systemId: string,tr:TimeAndDuration) => {

    let res: GraphDataDTO;

    try {
      res = await fetchLastFiveMinutes(systemId, tr.duration)
    }catch (ex){
      return false;
    }

    // @ts-ignore
    let newData: any[] = []
    // @ts-ignore
    let firstNewSampleDate =  res.data.length > 0 ? res.data[0].time : new Date();

    refGraphData.current?.data.forEach(d => {
      // @ts-ignore
      if (d.time > tr.start.getTime() && d.time < firstNewSampleDate) {
        newData.push(d)
      }
    })
    res.data.forEach(d => {
      newData.push(d)
    })

    //todo check if something changed on devices

    let devs = res.devices || [];

    if (refGraphData.current) {
      for (let devicesKey in refGraphData.current.devices) {
        if (devicesKey in devs) {
          devs[devicesKey].batteryIds = Array.from(new Set(res.devices[devicesKey].batteryIds.concat(refGraphData.current.devices[devicesKey].batteryIds)))
          devs[devicesKey].inputDCIds = Array.from(new Set(res.devices[devicesKey].inputDCIds.concat(refGraphData.current.devices[devicesKey].inputDCIds)))
          devs[devicesKey].inputACIds = Array.from(new Set(res.devices[devicesKey].inputACIds.concat(refGraphData.current.devices[devicesKey].inputACIds)))
          devs[devicesKey].outputDCIds = Array.from(new Set(res.devices[devicesKey].outputDCIds.concat(refGraphData.current.devices[devicesKey].outputDCIds)))
          devs[devicesKey].outputACIds = Array.from(new Set(res.devices[devicesKey].outputACIds.concat(refGraphData.current.devices[devicesKey].outputACIds)))
        } else {
          devs[devicesKey] = refGraphData.current.devices[devicesKey]
        }
      }
    }
    refGraphData.current = {data: newData, devices: devs}
    setGraphData(refGraphData.current)
    updateColors(res)
    return true;
  }

  const fetchFullGraphData = async (systemId: string,tr:TimeAndDuration) => {
    let r : GraphDataDTO;
    try {
      r = await getAllGraphData(systemId, tr.start.getTime(), tr.end.getTime())
    }catch(e){
      return false;
    }
    refGraphData.current = {data: r.data, devices: r.devices || []}
    setGraphData(refGraphData.current)
    updateColors(refGraphData.current)
    return true;
  }

  useEffect(() => {
    if(params.id){
      getSystem(""+params.id).then((res) => {
        if(res.viewData.batteryVoltage){
          if(res.viewData.batteryVoltage<20){
            setMinBV(res.viewData.batteryVoltage-2)
            setMaxBV(res.viewData.batteryVoltage+2)
          }else if(res.viewData.batteryVoltage<40){
            setMinBV(res.viewData.batteryVoltage-4)
            setMaxBV(res.viewData.batteryVoltage+4)
          }else if(res.viewData.batteryVoltage<60){
            setMinBV(res.viewData.batteryVoltage-6)
            setMaxBV(res.viewData.batteryVoltage+6)
          }else if(res.viewData.batteryVoltage<80){
            setMinBV(res.viewData.batteryVoltage-8)
            setMaxBV(res.viewData.batteryVoltage+8)
          }
        }
        setData(res)
        if(res?.status?.booleans){
          setBooleanStatus(res.status.booleans)
        }
        fetchFullGraphData(res.id,refTimeRange.current.time)
      })
     }
   },[])

  const saveGetColorByName = (name:string)=>{
    let res = colorsByName.get(name);
    if(!res){
      return "black"
    }
    return res;
  }

  return <div>
    {data ? <>
      <ContinuousUpdateWrapper fullReloadCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,new Date()),true,true)}
        active={timeRange.autoUpdate} updateCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,new Date()),true,false)}
         fetchTimout={1000 * 60} fullReloadTimeout={1000 * 60 * 3.5}/>
      {graphData && <div style={{display:"flex", justifyContent:"center"}}>
        <div style={{display:"flex",flexDirection:"column"}}>
        <h3>{data.name}</h3>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            <div style={{margin:"10px"}}>
              Timezone: {data.timezone}
            </div>
          </div>
          <TimeAndDateSelector onChange={(tr,nowButton)=>internUpdateTimeRange(tr.time,tr.autoUpdate,nowButton)} timeRange={timeRange} timeRanges={durations}/>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            Update: {timeRange.autoUpdate ? "on":"off"}
          </div>
          {//TODO find better way to do this
            data.status && <Button style={{marginTop: "auto", marginBottom: "auto"}} variant="contained" onClick={() => {
            navigate('/edit/System/'+data.id)
          }}>Edit System</Button>}
        </div>
        <div style={{maxWidth:"1490px",padding: "10px"}}>
          <CheckBoxComponentFilters devices={graphData.devices} showCombined={showCombined} setShowCombined={setShowCombined} getDeviceColour={saveGetColorByName}
                                  checkedDeviceIds={checkedDeviceIds} checkedInputDCIds={checkedInputDCIds} checkedInputACIds={checkedInputACIds}
                                  checkedOutputDCIds={checkedOutputDCIds} checkedOutputACIds={checkedOutputACIds} checkedBatteryIds={checkedBatteryIds}
                                  setCheckedDeviceIds={setCheckedDeviceIds} setCheckedInputDCIds={setCheckedInputDCIds} setCheckedInputACIds={setCheckedInputACIds}
                                  setCheckedOutputDCIds={setCheckedOutputDCIds} setCheckedOutputACIds={setCheckedOutputACIds} setCheckedBatteryIds={setCheckedBatteryIds}/>
        </div>
        <div style={{margin:"auto"}}>
          {<div className={"detailDashboard"}>
            {data.status?.booleans?.length > 0 &&
              <Accordion defaultExpanded={false} style={{backgroundColor:"lightsteelblue",marginBottom:"5px"}} className={"DetailAccordion"}>
                <AccordionSummary
                  expandIcon={<ExpandMoreIcon/>}
                  aria-controls="panel1a-content"
                  id="panel1a-header"
                >
                  <Typography>Status</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <SetStatusList horizontal={true} booleanStatus={booleanStatus} internalSetBooleanStatus={setBooleanStatus} loading={statusLoading} setLoading={setStatusLoading} systemId={data.id}/>
                </AccordionDetails>
              </Accordion>
            }
            <InputAccordion showAmpere={data.viewData.showAmpere} hasAC={!data.publicFlagOnlyProduction && data.viewData.hasACInput == true} inputDCIds={checkedInputDCIds} inputACIds={checkedInputACIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} maxSolarVoltage={data.viewData.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            {!data.publicFlagOnlyProduction && (data.type == SolarSystemType.SELFMADE || data.type == SolarSystemType.GRID_BATTERY) &&
              <BatteryAccordion showAmpere={data.viewData.showAmpere} batteryIds={checkedBatteryIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} isBatteryPercentage={data.viewData.isBatteryPercentage} timeRange={timeRange.time} graphData={graphData}/>
            }
            {!data.publicFlagOnlyProduction && (data.viewData.hasDCOutput || data.viewData.hasACOutput) &&
              <OutputAccordion showAmpere={data.viewData.showAmpere} hasAC={data.viewData.hasACOutput == true} hasDC={data.viewData.hasDCOutput == true} outputACIds={checkedOutputACIds} outputDCIds={checkedOutputDCIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} timeRange={timeRange.time} graphData={graphData}/>
            }
            <StatisticsAccordion systemInfo={data}/>
          </div>}
        </div>
        </div>
      </div>}
    </>:<CircularProgress/>}
  </div>
}

