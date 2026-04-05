import React, {useEffect, useRef, useState} from "react";
import {BooleanStatus, getSystem, getSystemInfo, SolarSystemDTO, SolarSystemType} from "../api/SolarSystemAPI";
import {useLocation, useNavigate, useParams, useSearchParams} from "react-router-dom";
import BatteryAccordion from "../Component/Accordions/BatteryAccordion";
import GridAccordion from "../Component/Accordions/GridAccordion";
import StatisticsAccordion from "../Component/Accordions/StatisticsAccordion"
import {fetchLastFiveMinutes, getAllGraphData, GraphDataDTO, DeviceGraphDataObject} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../Component/time/TimeAndDateSelector";
import InputAccordion from "../Component/Accordions/InputAccordion";
import OutputAccordion from "../Component/Accordions/OutputAccordion";
import {Accordion, AccordionDetails, AccordionSummary, Button, CircularProgress, Typography} from "@mui/material";
import {getGraphColourByIndex} from "../Component/utils/GraphUtils";
import DevicesCheckBoxComponentFilters from "../Component/DevicesCheckBoxComponentFilters";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import SetStatusList from "../Component/SetStatusList";
import ContinuousUpdateWrapper from "../Component/ContinuousUpdateWrapper";
import moment from "moment";
import MoreAccordion from "../Component/Accordions/MoreAccordion";
import TotalDataAccordion from "../Component/Accordions/TotalDataAccordion";
import {useTranslation} from "react-i18next";

export default function DetailDashboardComponent(){

  const { t } = useTranslation()

  const params = useParams()

  const [searchParams] = useSearchParams();

  const durations = ["5m","10m","30m","1h","3h","6h","12h","24h"]

  const durationPara = searchParams.get("duration")
  let initDuration = (durationPara && durations.includes(durationPara)) ? durationPara:"3h"
  let dateParam = searchParams.get("date")
  let initDate = null;
  if(dateParam){
    var d = moment(parseInt(dateParam))
    if(!isNaN(d.valueOf())){
      initDate = d
    }
  }

  const [graphData, setGraphData] = useState<DeviceGraphDataObject>()
  const refGraphData = useRef<DeviceGraphDataObject>()
  const [data, setData] = useState<SolarSystemDTO>()
  const refTimeRange = useRef({autoUpdate:true,time:generateTimeDuration(initDuration,initDate?initDate:moment())})
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
  const [checkedGridIds,setCheckedGridIds] = useState(new Set<string>())
  const [showCombined,setShowCombined] = useState(true)
  const [booleanStatus, setBooleanStatus] = useState<BooleanStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(false)

  const [viewNamings, setViewNamings] = useState({})

  const saveGetColorByName = (name:string)=>{
    let res = colorsByName.get(name);
    if(!res){
      return "black"
    }
    return res;
  }

  const upateViewNamings = (dto: SolarSystemDTO)=>{
    let res : {[key: string]: string} = {}

    for (const [key, value] of Object.entries(dto.namings.devices)) {
      res["InputWattDC-d-"+key] = "InputWattDC "+value;
      res["InputWattAC-d-"+key] = "InputWattAC "+value;
      res["InputVoltageDC-d-"+key] = "InputVoltageDC "+value;
      res["InputAmpereDC-d-"+key] = "InputAmpereDC "+value;
      res["InputVoltageAC-d-"+key] = "InputVoltageAC "+value;
      res["InputFrequency-d-"+key] = "InputFrequency "+value;

      res["OutputWattAC-d-"+key] = "OutputWattAC "+value;
      res["OutputVoltageDC-d-"+key] = "OutputVoltageDC "+value;
      res["OutputAmpereDC-d-"+key] = "OutputAmpereDC "+value;
      res["OutputVoltageAC-d-"+key] = "OutputVoltageAC "+value;
      res["OutputAmpereAC-d-"+key] = "OutputAmpereAC "+value;
      res["OutputFrequency-d-"+key] = "OutputFrequency "+value;

      res["BatteryWatt-d-"+key] = "BatteryWatt "+value;
      res["BatteryVoltage-d-"+key] = "BatteryVoltage "+value;
      res["BatteryAmpere-d-"+key] = "BatteryAmpere "+value;
    }
    for (const [key, value] of Object.entries(dto.namings.inputsDC)) {
      res["Watt-i-"+key] = "Watt "+value;
      res["Voltage-i-"+key] = "Voltage "+value;
      res["Ampere-i-"+key] = "Ampere "+value;
    }
    for (const [key, value] of Object.entries(dto.namings.inputsAC)) {
      res["Watt-j-"+key] = "Watt "+value;
      res["Voltage-j-"+key] = "Voltage "+value;
      res["Ampere-j-"+key] = "Ampere "+value;
      res["Frequency-j-"+key] = "Frequency "+value;
    }

    for (const [key, value] of Object.entries(dto.namings.outputsDC)) {
      res["Watt-o-"+key] = "Watt "+value;
      res["Voltage-o-"+key] = "Voltage "+value;
      res["Ampere-o-"+key] = "Ampere "+value;
    }
    for (const [key, value] of Object.entries(dto.namings.outputsAC)) {
      res["Watt-c-"+key] = "Watt "+value;
      res["Voltage-c-"+key] = "Voltage "+value;
      res["Ampere-c-"+key] = "Ampere "+value;
      res["Frequency-c-"+key] = "Frequency "+value;
    }

    for (const [key, value] of Object.entries(dto.namings.batteries)) {
      res["Watt-b-"+key] = "Watt "+value;
      res["Voltage-b-"+key] = "Voltage "+value;
      res["Ampere-b-"+key] = "Ampere "+value;
    }

    for (const [key, value] of Object.entries(dto.namings.grids)) {
      res["Watt-g-"+key] = "Watt "+value;
      res["Voltage-g-"+key] = "Voltage "+value;
      res["Ampere-g-"+key] = "Ampere "+value;
    }

    console.log(res)

    setViewNamings(res)
  }

  const navigate = useNavigate()
  const location = useLocation()

  const internUpdateTimeRange = async (newTimeRange: TimeAndDuration, autoUpdate: boolean, forceReload?: boolean) => {//TODO replace any
    navigate({
      pathname: location.pathname,
      search: "?duration=" + newTimeRange.durationString + (!autoUpdate ? "&date=" + newTimeRange.end.valueOf() : ""),
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

  const updateColors = (data:DeviceGraphDataObject)=>{
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
      for (let id of data.devices[devicesKey].gridIds) {
        colors.set("g-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
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
    let firstNewSampleDate =  res.data.length > 0 ? res.data[0].time : moment();

    refGraphData.current?.data.forEach(d => {
      // @ts-ignore
      if (d.time > tr.start.valueOf() && d.time < firstNewSampleDate) {
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
          devs[devicesKey].gridIds = Array.from(new Set(res.devices[devicesKey].gridIds.concat(refGraphData.current.devices[devicesKey].gridIds)))
        } else {
          devs[devicesKey] = refGraphData.current.devices[devicesKey]
        }
      }
    }
    refGraphData.current = {data: newData, devices: devs,totalData: res.totalData}
    setGraphData(refGraphData.current)
    //@ts-ignore
    updateColors(refGraphData.current)
    return true;
  }

  const fetchFullGraphData = async (systemId: string,tr:TimeAndDuration) => {
    let r : GraphDataDTO;
    try {
      r = await getAllGraphData(systemId, tr.start.valueOf(), tr.end.valueOf())
    }catch(e){
      console.log(e)
      return false;
    }
    refGraphData.current = {data: r.data, devices: r.devices || [],totalData: r.totalData}
    setGraphData(refGraphData.current)
    updateColors(refGraphData.current)
    return true;
  }

  useEffect(() => {
    if(params.id){
      getSystemInfo(""+params.id).then((res) => {
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
        upateViewNamings(res);
        if(res?.status?.booleans){
          setBooleanStatus(res.status.booleans)
        }
        fetchFullGraphData(res.id,refTimeRange.current.time)
      })
     }
   },[])

  return <div>
    {data ? <>
      <ContinuousUpdateWrapper fullReloadCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
        active={timeRange.autoUpdate} updateCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
         fetchTimout={1000 * 60} fullReloadTimeout={1000 * 60 * 3.5}/>
      {graphData ? <div style={{display:"flex", justifyContent:"center"}}>
        <div style={{display:"flex",flexDirection:"column"}}>
        <h2>{t("common.system")+": "+data.viewName}{data.status &&
          <Button style={{marginLeft:"5px", marginTop: "auto", marginBottom: "auto"}} variant="contained" onClick={() => {
            navigate('/edit/System/'+data.id)
          }}>{t("views.dashboard.edit_system")}</Button>
        }</h2>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            <div style={{margin:"10px"}}>
              {t("common.timezone")}: {data.timezone}
            </div>
          </div>
          <TimeAndDateSelector timezone={data.timezone} onChange={(tr,nowButton)=>internUpdateTimeRange(tr.time,tr.autoUpdate,nowButton)} timeRange={timeRange} timeRanges={durations}/>
        </div>
        <div style={{maxWidth:"1490px",padding: "10px"}}>
          <DevicesCheckBoxComponentFilters namings={data.namings} devices={graphData.devices} showCombined={showCombined} setShowCombined={setShowCombined} getDeviceColour={saveGetColorByName}
                                           checkedDeviceIds={checkedDeviceIds} checkedInputDCIds={checkedInputDCIds} checkedInputACIds={checkedInputACIds}
                                           checkedOutputDCIds={checkedOutputDCIds} checkedOutputACIds={checkedOutputACIds} checkedBatteryIds={checkedBatteryIds} checkedGridIds={checkedGridIds}
                                           setCheckedDeviceIds={setCheckedDeviceIds} setCheckedInputDCIds={setCheckedInputDCIds} setCheckedInputACIds={setCheckedInputACIds}
                                           setCheckedOutputDCIds={setCheckedOutputDCIds} setCheckedOutputACIds={setCheckedOutputACIds} setCheckedBatteryIds={setCheckedBatteryIds} setCheckedGridIds={setCheckedGridIds}/>
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
                  <Typography>{t("common.status")}</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <SetStatusList horizontal={true} booleanStatus={booleanStatus} internalSetBooleanStatus={setBooleanStatus} loading={statusLoading} setLoading={setStatusLoading} systemId={data.id}/>
                </AccordionDetails>
              </Accordion>
            }
            <TotalDataAccordion solarSystem={data} graphData={graphData}/>
            <InputAccordion graphFilter={data.viewData.graphFilter} defaultDuration={data.viewData.defaultDelay} namings={viewNamings} inputDCIds={checkedInputDCIds} inputACIds={checkedInputACIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} maxSolarVoltage={data.viewData.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            {!data.publicFlagOnlyProduction && (data.type == SolarSystemType.SELFMADE || data.type == SolarSystemType.GRID_BATTERY) &&
              <BatteryAccordion graphFilter={data.viewData.graphFilter} defaultDuration={data.viewData.defaultDelay}  namings={viewNamings} batteryIds={checkedBatteryIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} timeRange={timeRange.time} graphData={graphData}/>
            }
            {!data.publicFlagOnlyProduction && (data.type == SolarSystemType.GRID || data.type == SolarSystemType.GRID_BATTERY) && data.viewData.showGridInfo === true &&
              <GridAccordion graphFilter={data.viewData.graphFilter} defaultDuration={data.viewData.defaultDelay} namings={viewNamings} timezone={data.timezone} timeRange={timeRange.time} graphData={graphData} deviceIds={checkedDeviceIds} gridIds={checkedGridIds} showCombined={showCombined} getDeviceColour={saveGetColorByName}/>
            }
            {!data.publicFlagOnlyProduction &&
              <OutputAccordion graphFilter={data.viewData.graphFilter} defaultDuration={data.viewData.defaultDelay} namings={viewNamings} outputACIds={checkedOutputACIds} outputDCIds={checkedOutputDCIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} timeRange={timeRange.time} graphData={graphData}/>
            }
            {/*TODO later add more conditions*/}
            {data.viewData.hasTemperature === true &&
              <MoreAccordion graphFilter={data.viewData.graphFilter} defaultDuration={data.viewData.defaultDelay} namings={viewNamings} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} timeRange={timeRange.time} graphData={graphData}/>
            }
            <StatisticsAccordion systemInfo={data}/>
          </div>}
        </div>
        </div>
      </div>:<><CircularProgress/> {t("common.loading.graph")}</>}
    </>:<><CircularProgress/> {t("common.loading.system")}</>}
  </div>
}

