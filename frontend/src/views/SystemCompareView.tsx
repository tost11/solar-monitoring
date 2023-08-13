import ContinuousUpdateWrapper from "../Component/ContinuousUpdateWrapper";
import {
  getMultSystems,
  MultSolarSystemDTO,
} from "../api/SolarSystemAPI";
import React, {useEffect, useRef, useState} from "react";
import {
  fetchLastFiveMinutesCombined,
  getAllCombinedGraphData,
  GraphDataDTO,
  GraphDataObject
} from "../api/GraphAPI";
import {CircularProgress} from "@mui/material";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../Component/time/TimeAndDateSelector";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import moment from "moment";
import LineGraph from "../Component/LineGraph";
import CombinedStatisticsAccordion from "../Component/Accordions/CombinedStatisticsAccordion";

export default function SystemCompareView() {

  const navigate = useNavigate()

  const [searchParams, setSearchParams] = useSearchParams();
  const durations = ["5m","10m","30m","1h","3h","6h","12h","24h"]
  const durationPara = searchParams.get("duration")
  let initDuration = (durationPara && durations.includes(durationPara)) ? durationPara:"3h"
  let dateParam = searchParams.get("date")
  let paramSystemIds = searchParams.getAll("systemIds")
  let initDate = null;
  if(dateParam){
    var d = moment(parseInt(dateParam))
    if(!isNaN(d.valueOf())){
      initDate = d
    }
  }

  const refGraphData = useRef<GraphDataObject>()
  const [systems, setSystems] = useState<{data:MultSolarSystemDTO[]}>()
  const [graphData, setGraphData] = useState<GraphDataObject>()
  const refTimeRange = useRef({autoUpdate:true,time:generateTimeDuration(initDuration,initDate?initDate:moment())})
  const [timeRange,setTimeRange] = useState(refTimeRange.current)
  const [systemMappings,setSystemMappingss] = useState<{[key: string]: string}>({})
  const [initSystemIds,] = useState(paramSystemIds)

  const fetchFullGraphData = async (systemIds: string[],tr:TimeAndDuration) => {
    let r : GraphDataDTO;
    try {
      r = await getAllCombinedGraphData(systemIds, tr.start.valueOf(), tr.end.valueOf())
    }catch(e){
      console.log(e)
      return false;
    }
    refGraphData.current = {data: r.data}
    setGraphData(refGraphData.current)
    return true;
  }

  const getIds = (systems:MultSolarSystemDTO[]) => {
    return systems.map(r=>r.id)
  }

  useEffect(()=>{
    //setInitSystemIds(paramSystemIds)
    if(! paramSystemIds || paramSystemIds.length < 1){
      setSystems({data:[]})
      return;
    }
    getMultSystems(paramSystemIds).then(ret=>{
      setSystems({data:ret})
      var mappings = {}
      for (let i = 0; i < ret.length; i++) {
        mappings["InputWatt_"+i] = ret[i].viewName;
        mappings["Produced_"+i] = ret[i].viewName;
        mappings["Consumed_"+i] = ret[i].viewName;
      }
      setSystemMappingss(mappings);
      fetchFullGraphData(getIds(ret),refTimeRange.current.time)
    });
  },[])

  const getLabels = (startPattern:string)=>{
    let ret = [];
    for (let i = 0; i < systems?.data.length; i++) {
      ret.push(startPattern + i);
    }
    return ret;
  }

  const continuousUpdateDataCallback = async (systemIds: string[],tr:TimeAndDuration) => {

    let res: GraphDataObject;

    try {
      res = await fetchLastFiveMinutesCombined(systemIds, tr.duration)
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

    refGraphData.current = {data: newData}
    setGraphData(refGraphData.current)
    return true;
  }

  const internUpdateTimeRange = async (newTimeRange: TimeAndDuration, autoUpdate: boolean, forceReload?: boolean) => {//TODO replace any

    navigate({
      pathname: location.pathname,
      search: "?duration=" + newTimeRange.durationString
        + "&" + initSystemIds.map(e=>"systemIds="+e).join("&")
        + (!autoUpdate ? "&date=" + newTimeRange.end.valueOf() : "")
    }, {replace: true})
    let fullFetch = forceReload || autoUpdate == false || (refTimeRange.current.autoUpdate == false && autoUpdate == true) || newTimeRange.duration != refTimeRange.current.time.duration
    refTimeRange.current = {autoUpdate: autoUpdate, time: newTimeRange}
    setTimeRange(refTimeRange.current)
    if (fullFetch) {
      // @ts-ignore
      return await fetchFullGraphData(getIds(systems?.data),newTimeRange)
    } else {
      // @ts-ignore
      return await continuousUpdateDataCallback(getIds(systems?.data),newTimeRange)
    }
  }

  return <div>

    {systems ? <>
      {systems.data.length > 0 ? <>
        <ContinuousUpdateWrapper fullReloadCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
                               active={timeRange.autoUpdate} updateCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
                               fetchTimout={1000 * 60} fullReloadTimeout={1000 * 60 * 3.5}/>
        {graphData ? <>
          <div style={{display:"flex",flexDirection:"column"}}>
            <h3>Combined systems</h3>
            <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
              <TimeAndDateSelector onChange={(tr,nowButton)=>internUpdateTimeRange(tr.time,tr.autoUpdate,nowButton)} timeRange={timeRange} timeRanges={durations}/>
              <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
                Update: {timeRange.autoUpdate ? "on":"off"}
              </div>
            </div>
            <div style={{display:"flex",alignContent:"center",marginTop:"15px"}}>
              <div className="fakeAccordion">
                <LineGraph valueNameOverrides={systemMappings} legendOverrideValue={"Input Power in Watt"} min={0}
                           timeRange={refTimeRange.current.time} graphData={graphData} unit="W"
                           labels={getLabels("InputWatt_")} />
              </div>
              </div>
          </div>
            <CombinedStatisticsAccordion systemNamings={systemMappings} systemInfos={systems.data}/>
          </>
          :
          <>
            <CircularProgress/> Loading Graph Data
          </>}
          </>:
          <>
            No Graph data specified to Load
          </>}
        </>:
        <>:
        <CircularProgress/> Loading Systems info
      </>
    }

    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}


  </div>
}
