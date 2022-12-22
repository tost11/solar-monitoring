import React, {useEffect, useLayoutEffect, useState} from "react";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useLocation, useNavigate, useParams, useSearchParams} from "react-router-dom";
import SolarPanelAccordion from "../Component/Accordions/SolarPanelAccordion";
import BatteryAccordion from "../Component/Accordions/BatteryAccordion";
import StatisticsAccordion from "../Component/Accordions/StatisticsAccordion"
import ConsumptionAccordion from "../Component/Accordions/ConsumptionAccordion";
import {DeviceIdsWrapper, fetchLastFiveMinutes, getAllGraphData, GraphDataDTO} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration} from "../Component/time/TimeAndDateSelector";
import InputAccordion from "../Component/Accordions/InputAccordion";
import OutputAccordion from "../Component/Accordions/OutputAccordion";
import {Checkbox, CircularProgress, FormControlLabel} from "@mui/material";
import {getGraphColourByIndex} from "../Component/utils/GraphUtils";

export interface GraphDataObject{
  data:any[]
  timer?:any,
  devices: DeviceIdsWrapper
}

interface Colors{
  main: string[],
  devices: string[],
  inputs: string[],
  outputs: string[],
  batteries: string[]
}

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

  /*const shouldUpdateBeEnabled = (date:Date) => {
    const treeMinutesAgo = moment().subtract(3, 'minutes')
    return treeMinutesAgo.isBefore(moment(date))
  }*/

  const [data, setData] = useState<SolarSystemDTO>()
  const [graphData,setGraphData]=useState<GraphDataObject>()
  const [timeRange,setTimeRange] = useState({fromInterval:false,time:generateTimeDuration(initDuration,initDate?initDate:new Date())})
  const [minBV,setMinBV] = useState<number>()
  const [maxBV,setMaxBV] = useState<number>()
  const [checkedDeviceIds,setCheckedDeviceIds] = useState(new Set<string>())
  const [checkInputIds,setCheckedInputIds] = useState(new Set<string>())
  const [checkOutputIds,setCheckedOutputIds] = useState(new Set<string>())
  //const [colors,setColors] = useState({main:[],devices:[],inputs:[],outputs:[],batteries:[]})
  const [colorsByName,setColorsByName] = useState(new Map<string,string>())
  const [checkedBatteryIds,setCheckedBatteryIds] = useState(new Set<string>())
  const [showCombined,setShowCombined] = useState(true)
  const [isUpdateEnabled, setUpdateEnabled] = useState(initDate === null)

  const navigate = useNavigate()
  const location = useLocation()

  const internUpdateTimeRange = (timeRange:any,overrideUpdateValue? :boolean)=>{//TODO replace any
    let autoUpdate = isUpdateEnabled
    if(!timeRange.fromInterval) {
      if(overrideUpdateValue != undefined){
        autoUpdate = overrideUpdateValue;
      }
      setUpdateEnabled(autoUpdate)
    }
    navigate({
      pathname: location.pathname,
      search: "?duration="+timeRange.time.durationString+(!autoUpdate?"&date="+timeRange.time.end.getTime():""),
    },{replace:true});
    setTimeRange(timeRange)
  }

  const timeoutCallback = () => {
    if(!isUpdateEnabled){
      console.log("skipped timer call because side state is no more auto update ")
      return;
    }
    internUpdateTimeRange({
      fromInterval: true,
      time: generateTimeDuration(timeRange.time.durationString, new Date())
    })
  }

  const updateColors = (data:GraphDataDTO)=>{
    //let colors = {main:[],devices:[],inputs:[],outputs:[],batteries:[]}
    let colors = new Map<string,string>()

    let i = 1;
    //colors.main.push(getGraphColourByIndex(i++))
    for (let devicesKey in data.devices) {
      //colors.devices.push(getGraphColourByIndex(i++))
      colors.set("d-"+devicesKey,getGraphColourByIndex(i++))
    }
    for (let devicesKey in data.devices) {
      for (let id of data.devices[devicesKey].inputIds) {
        //colors.inputs.push(getGraphColourByIndex(i++))
        colors.set("i-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].outputIds) {
        //colors.outputs.push(getGraphColourByIndex(i++))
        colors.set("o-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
      for (let id of data.devices[devicesKey].batteryIds) {
        //colors.batteries.push(getGraphColourByIndex(i++))
        colors.set("b-"+devicesKey+"-"+id,getGraphColourByIndex(i++))
      }
    }
    // @ts-ignore
    console.log("Colors: ",colors)
    setColorsByName(colors)
  }

  const updateGraphData = (systemId:number) => {
    if(!data){
      return
    }
    fetchLastFiveMinutes(systemId,timeRange.time.duration).then(res=>{
      // @ts-ignore
      let newData:any[] = []
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
      let timer = setTimeout(timeoutCallback,1000 * 60)
      console.log("Start new timeout ",timer)

      //handle new deviceIds TODO fix
      /*let newDevices = new Set<number>()
      graphData?.deviceIds?.forEach(d=>newDevices.add(d))
      res.deviceIds?.forEach(d=>{
        if(newDevices.has(d) === false){
          newDevices.add(d)
        }
      })*/

      //todo check if something changed on devices

      let devs = res.devices;

      if(graphData) {
        for (let devicesKey in graphData.devices) {
          if ((devicesKey in res.devices)) {
            devs[devicesKey].batteryIds = Array.from(new Set(res.devices[devicesKey].batteryIds.concat(graphData.devices[devicesKey].batteryIds)))
            devs[devicesKey].inputIds = Array.from(new Set(res.devices[devicesKey].inputIds.concat(graphData.devices[devicesKey].inputIds)))
            devs[devicesKey].outputIds = Array.from(new Set(res.devices[devicesKey].outputIds.concat(graphData.devices[devicesKey].outputIds)))
          }else{
            devs[devicesKey] = graphData.devices[devicesKey]
          }
        }
      }
      setGraphData({data:newData,devices: devs,timer:timer})
      updateColors(res)
    })
  }

  const checkGraphData = (res:SolarSystemDTO) => {
    if(timeRange.fromInterval){
      updateGraphData(res.id)
    }else{
      if(graphData && graphData.timer){
        clearTimeout(graphData.timer)
      }
      // @ts-ignore
      getAllGraphData(res.id,timeRange.time.start.getTime(), timeRange.time.end.getTime()).then((r)=>{
        let timer = undefined;
        if(isUpdateEnabled) {
          timer = setTimeout(timeoutCallback, 1000 * 60)
          console.log("Start new timeout ",timer)
        }
        let d = {data:r.data,devices:r.devices,timer:timer}
        setGraphData(d)
        updateColors(d)
      })
    }
  }

  useEffect(() => {
    return function cleanup(){
      //console.log("Component dismount")
      //console.log(graphData)
      if(graphData !=undefined && graphData.timer){
        console.log("Clearing timer (may be old)",graphData.timer)
        clearTimeout(graphData.timer)
      }
    };
  },[graphData]);

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
        })
     }
   }, [timeRange])

  const changeIdSelection = (id:string,on:Set<string>,set:(v:Set<string>)=>void)=>{
    var newSelection = new Set<string>(on)
    if(newSelection.has(id)){
      newSelection.delete(id)
    }else{
      newSelection.add(id)
    }
    set(newSelection)
  }

  const saveGetColorByName = (name:string)=>{
    let res = colorsByName.get(name);
    if(!res){
      return "black"
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
          <TimeAndDateSelector maxDate={new Date()} onChange={(v,now,dur)=>{
            let override = now ? true : (dur?undefined:false)
            internUpdateTimeRange({fromInterval: false,time:v },override)
          }} timeRange={timeRange.time} timeRanges={durations}/>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            Update: {graphData.timer != undefined ? "on":"off"}
          </div>
        </div>
        {graphData?.devices && <div className="defaultFlex">
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
          {
            Object.entries(graphData.devices).map(([k,v],i)=>{
              return <><FormControlLabel
                key={i}
                label={<div style={{color: saveGetColorByName("d-"+k)}}>{"Device "+k}</div>}
                control={<Checkbox
                  checked={checkedDeviceIds.has(""+k)}
                  onChange={()=>changeIdSelection(k,checkedDeviceIds,setCheckedDeviceIds)}
                  inputProps={{ 'aria-label': 'controlled' }}
                />}
              />{v.inputIds.length > 0 && <div style={{background:"white"}}>
                  {v.inputIds.map((id,i2)=>{
                    return <FormControlLabel
                      key={i2}
                      label={<div style={{color: saveGetColorByName("i-"+k+"-"+id)}}>{"Input "+id}</div>}
                      control={<Checkbox
                        checked={checkInputIds.has(""+k+"-"+id)}
                        onChange={()=>changeIdSelection(""+k+"-"+id,checkInputIds,setCheckedInputIds)}
                        inputProps={{ 'aria-label': 'controlled' }}
                      />}
                    />
                  })
                }
              </div>}
              {v.batteryIds.length > 0 && <div style={{background:"white"}}>
                {v.batteryIds.map((id,i2)=>{
                  return <FormControlLabel
                      key={i2}
                      label={<div style={{color: saveGetColorByName("b-"+k+"-"+id)}}>{"Battery "+id}</div>}
                      control={<Checkbox
                          checked={checkedBatteryIds.has(""+k+"-"+id)}
                          onChange={()=>changeIdSelection(""+k+"-"+id,checkedBatteryIds,setCheckedDeviceIds)}
                          inputProps={{ 'aria-label': 'controlled' }}
                      />}
                  />
                })
                }
              </div>}
              {v.outputIds.length > 0 && <div style={{background:"white"}}>
                  {v.outputIds.map((id,i2)=>{
                    return <FormControlLabel
                      key={i2}
                      label={<div style={{color: saveGetColorByName("o-"+k+"-"+id)}}>{"Output "+id}</div>}
                      control={<Checkbox
                        checked={checkOutputIds.has(""+k+"-"+id)}
                        onChange={()=>changeIdSelection(""+k+"-"+id,checkOutputIds,setCheckedOutputIds)}
                        inputProps={{ 'aria-label': 'controlled' }}
                      />}
                    />
                  })
                }
              </div>}
            </>
          })}
        </div>}
        <div>
          {<div className={"detailDashboard"}>
            <InputAccordion inputIds={checkInputIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            { data.type != "GRID" &&
              data.type != "VERY_SIMPLE" &&
              data.type != "VERY_SIMPLE" &&
              <BatteryAccordion batteryIds={checkedBatteryIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} isBatteryPercentage={data.isBatteryPercentage} timeRange={timeRange.time} graphData={graphData}/>}
            { data.type != "VERY_SIMPLE" &&
              data.type != "SIMPLE" &&
              <OutputAccordion outputIds={checkOutputIds} deviceIds={checkedDeviceIds} timezone={data.timezone} getDeviceColour={saveGetColorByName} showCombined={showCombined} timeRange={timeRange.time} graphData={graphData}/>}
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
        </div>
      </div>
    </div>:<CircularProgress/>}
  </div>
}

