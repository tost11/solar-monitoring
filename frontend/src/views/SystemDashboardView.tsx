import React, {useEffect, useLayoutEffect, useState} from "react";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useLocation, useNavigate, useParams, useSearchParams} from "react-router-dom";
import SolarPanelAccordion from "../Component/Accordions/SolarPanelAccordion";
import BatteryAccordion from "../Component/Accordions/BatteryAccordion";
import StatisticsAccordion from "../Component/Accordions/StatisticsAccordion"
import ConsumptionAccordion from "../Component/Accordions/ConsumptionAccordion";
import {DeviceIdsWrapper, fetchLastFiveMinutes, getAllGraphData} from "../api/GraphAPI";
import TimeAndDateSelector, {generateTimeDuration} from "../Component/time/TimeAndDateSelector";
import InputAccordion from "../Component/Accordions/InputAccordion";
import GridOutputAccordion from "../Component/Accordions/GridOutputAccordion";
import {Checkbox, CircularProgress, FormControlLabel} from "@mui/material";
import {getGraphColourByIndex} from "../Component/utils/GraphUtils";

export interface GraphDataObject{
  data:[]
  timer?:any,
  devices: DeviceIdsWrapper
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

  const updateGraphData = (systemId:number) => {
    if(!data){
      return
    }
    fetchLastFiveMinutes(systemId,timeRange.time.duration).then(res=>{
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

      // @ts-ignore
      setGraphData({data:newData,devices: res.devices,timer:timer})
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
        setGraphData({data:r.data,devices:r.devices,timer:timer})
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

  const changeIdSelection = (id:string,on,set)=>{
    var newSelection = new Set<string>(on)
    if(newSelection.has(id)){
      newSelection.delete(id)
    }else{
      newSelection.add(id)
    }
    set(newSelection)
  }

  const getColoursOfSelectedDevices = () => {
    /*if(!graphData || !graphData.deviceIds || graphData.deviceIds.length <= 0){
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
    }*/
    return [getGraphColourByIndex(0)];
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
                label={<div style={{color:getGraphColourByIndex(i+1)}}>{"Device "+k}</div>}
                control={<Checkbox
                  checked={checkedDeviceIds.has(""+k)}
                  onChange={()=>changeIdSelection(k,checkedDeviceIds,setCheckedDeviceIds)}
                  inputProps={{ 'aria-label': 'controlled' }}
                />}
              />{v.inputIds.length > 0 && <div style={{background:"white"}}>
                  {v.inputIds.map((id,i2)=>{
                    return <FormControlLabel
                      key={i}
                      label={<div style={{color:getGraphColourByIndex(i2)}}>{"Input "+id}</div>}
                      control={<Checkbox
                        checked={checkInputIds.has(""+k+"-"+id)}
                        onChange={()=>changeIdSelection(""+k+"-"+id,checkInputIds,setCheckedInputIds)}
                        inputProps={{ 'aria-label': 'controlled' }}
                      />}
                    />
                  })
                }
              </div>}
              {(v.outputIds > 0) > 0 && <div style={{background:"white"}}>
                  {v.outputIds.map((id,i2)=>{
                    return <FormControlLabel
                      key={i}
                      label={<div style={{color:getGraphColourByIndex(i2)}}>{"Output "+id}</div>}
                      control={<Checkbox
                        checked={checkOutputIds.has(""+k+"-"+id)}
                        onChange={()=>changeIdSelection(""+k+"-"+id,checkOutputIds,setCheckedOutputIds}
                        inputProps={{ 'aria-label': 'controlled' }}
                      />}
                    />
                  })
                }
              </div>}
                {(v.batteryIds > 0) > 0 && <div style={{background:"white"}}>
                  {v.batteryIds.map((id,i2)=>{
                    return <FormControlLabel
                      key={i}
                      label={<div style={{color:getGraphColourByIndex(i2)}}>{"Battery "+id}</div>}
                      control={<Checkbox
                        checked={checkedBatteryIds.has(""+k+"-"+id)}
                        onChange={()=>changeIdSelection(""+k+"-"+id,checkedBatteryIds,setCheckedDeviceIds)}
                        inputProps={{ 'aria-label': 'controlled' }}
                      />}
                    />
                  })
                }
              </div>
              }
              </>
            })}
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
            <InputAccordion inputIds={checkInputIds} deviceIds={checkedDeviceIds} timezone={data.timezone} deviceColours={getColoursOfSelectedDevices()} showCombined={showCombined} maxSolarVoltage={data.maxSolarVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <GridOutputAccordion timezone={data.timezone} deviceColours={getColoursOfSelectedDevices()} showCombined={showCombined} gridVoltage={data.inverterVoltage} timeRange={timeRange.time} graphData={graphData}/>
            <StatisticsAccordion systemInfo={data} consumption={false}/>
          </div>}
        </div>
      </div>
    </div>:<CircularProgress/>}
  </div>
}

