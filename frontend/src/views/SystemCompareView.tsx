import ContinuousUpdateWrapper, {ContinuousUpdateWrapperProps} from "../Component/ContinuousUpdateWrapper";
import {
  getMultSystems,
  getPublicSystems,
  getSystems,
  MultSolarSystemDTO,
  SolarSystemListDTO
} from "../api/SolarSystemAPI";
import React, {useEffect, useRef, useState} from "react";
import {
  fetchLastFiveMinutes,
  getAllCombinedGraphData,
  getAllGraphData,
  GraphDataDTO,
  GraphDataObject
} from "../api/GraphAPI";
import {Button, CircularProgress} from "@mui/material";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../Component/time/TimeAndDateSelector";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import moment from "moment";
import LineGraph from "../Component/LineGraph";

export default function SystemCompareView() {

  const navigate = useNavigate()

  const params = useParams()
  const [searchParams, setSearchParams] = useSearchParams();
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

  const refGraphData = useRef<GraphDataObject>()
  const [systems, setSystems] = useState<{data:MultSolarSystemDTO[]}>()
  const [graphData, setGraphData] = useState<GraphDataObject>()
  const refTimeRange = useRef({autoUpdate:true,time:generateTimeDuration(initDuration,initDate?initDate:moment())})
  const [timeRange,setTimeRange] = useState(refTimeRange.current)

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
    getMultSystems(["646a7e0184a89e6046ec58d7","646a7e0284a89e6046ec58d8"]).then(ret=>{
      setSystems({data:ret})
      fetchFullGraphData(getIds(ret),refTimeRange.current.time)
    });
  },[])



  const continuousUpdateDataCallback = async (systemId: string,tr:TimeAndDuration) => {

    //add implementation
    return true;

    let res: GraphDataObject;

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

    refGraphData.current = {data: newData}
    setGraphData(refGraphData.current)
    return true;
  }

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
      return await fetchFullGraphData(getIds(systems?.data),newTimeRange)
    } else {
      // @ts-ignore
      return await continuousUpdateDataCallback(getIds(systems?.data),newTimeRange)
    }
  }

  return <div>

    {systems ? <>
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
        </div>
        <div style={{margin:"auto"}}>
          <div className={"detailDashboard"}>
            <div className="defaultPanelWrapper">
              <LineGraph legendOverrideValue={"Input Power in Watt"} min={0} timeRange={refTimeRange.current.time} graphData={graphData} unit="W" labels={["InputWatt_0","InputWatt_1"]} />
            </div>
          </div>
        </div>
        </>
        :
        <>
          <CircularProgress/> Loading Graph Data
        </>}
      </>:
      <>
        <CircularProgress/> Loading Systems info
      </>
    }

    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}


  </div>
}
