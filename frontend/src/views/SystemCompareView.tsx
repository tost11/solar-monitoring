import ContinuousUpdateWrapper from "../Component/ContinuousUpdateWrapper";
import {
  getMultSystems,
  MultSolarSystemDTO,
} from "../api/SolarSystemAPI";
import React, {useContext, useEffect, useRef, useState} from "react";
import {
  DeviceGraphDataObject,
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
import DevicesCheckBoxComponentFilters from "../Component/DevicesCheckBoxComponentFilters";
import CheckBoxComponentFilters from "../Component/CheckBoxComponentFilters";
import {getGraphColourByIndex} from "../Component/utils/GraphUtils";
import {UserContext} from "../context/UserContext";
import {useTranslation} from "react-i18next";

export default function SystemCompareView() {

  const { t } = useTranslation()

  const navigate = useNavigate()
  const login = useContext(UserContext);

  const [searchParams, setSearchParams] = useSearchParams();
  const durations = ["5m","10m","30m","1h","3h","6h","12h","24h"]
  const durationPara = searchParams.get("duration")
  let initDuration = (durationPara && durations.includes(durationPara)) ? durationPara:"3h"
  let dateParam = searchParams.get("date")
  let paramSystemIds = searchParams.getAll("systemIds")
  let paramSys = searchParams.getAll("sys")
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
  const [systemMappings,setSystemMappings] = useState<{[key: string]: string}>({})
  const [initSystemIds,] = useState(paramSystemIds)
  const [colorsById,setColorsById] = useState(new Map<string,string>())
  const [checkedSystemIds,setCheckedSystemIds] = useState(new Set<string>())

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

  const getSysIdsFromParams = () => {
    let sysIds = new Set<string>();
    if(paramSystemIds){
      paramSystemIds.forEach(v=>sysIds.add(v))
    }
    if(paramSys){
      paramSys.forEach(v=>sysIds.add(v))
    }
    return Array.from(sysIds);
  }

  useEffect(()=>{
    //setInitSystemIds(paramSystemIds)
    if((! paramSystemIds || paramSystemIds.length < 1) && (!paramSys || paramSys.length < 1)){
      setSystems({data:[]})
      return;
    }

    let sysIds= getSysIdsFromParams();
    getMultSystems(sysIds,!login).then(ret=>{
      setSystems({data:ret})
      updateColors(ret)
      var mappings = {}
      var checkedIds = new Set<string>();
      for (let i = 0; i < ret.length; i++) {
        mappings["InputWatt_"+i] = ret[i].viewName;
        mappings["Produced_"+i] = ret[i].viewName;
        mappings["Consumed_"+i] = ret[i].viewName;
        mappings[ret[i].id] = ret[i].viewName;
        checkedIds.add(ret[i].id);
      }
      setCheckedSystemIds(checkedIds);
      setSystemMappings(mappings);
      fetchFullGraphData(getIds(ret),refTimeRange.current.time)
    });
  },[])

  const getLabels = (startPattern:string)=>{
    let ret = [];
    for (let i = 0; i < systems?.data.length; i++) {
      //@ts-ignore
      if(checkedSystemIds.has(systems.data[i].id)) {
        ret.push(startPattern + i);
      }
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
      if (d.time >= tr.start.valueOf() && d.time < firstNewSampleDate) {
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

  const getActiveColours = ()=>{
    let colors = [];
    for (let i = 0; i < systems?.data.length; i++) {
      //@ts-ignore
      if(checkedSystemIds.has(systems.data[i].id)) {
        colors.push(getGraphColourByIndex(i))
      }
    }
    return colors;
  }

  const internUpdateTimeRange = async (newTimeRange: TimeAndDuration, autoUpdate: boolean, forceReload?: boolean) => {//TODO replace any

    navigate({
      pathname: location.pathname,
      search: "?duration=" + newTimeRange.durationString
        + "&" + getSysIdsFromParams().map(e=>"sys="+e).join("&")
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

  const updateColors = (syses:MultSolarSystemDTO[])=>{
    //let colors = {main:[],devices:[],inputs:[],outputs:[],batteries:[]}
    let colors = new Map<string,string>()

    let i = 0;
    syses.forEach(v=>{
      colors.set(v.id,getGraphColourByIndex(i++))
    })
    setColorsById(colors)
  }

  const saveGetColorByName = (name:string)=>{
    let res = colorsById.get(name);
    if(!res){
      return "black"
    }
    return res;
  }

  return <div>

    {systems ? <>
      {systems.data.length > 0 ? <>
        <ContinuousUpdateWrapper fullReloadCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
                               active={timeRange.autoUpdate} updateCallback={()=>internUpdateTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
                               fetchTimout={1000 * 60} fullReloadTimeout={1000 * 60 * 3.5}/>
        {graphData ? <>
          <div style={{display:"flex",alignContent:"center"}}>
            <div style={{width:"100%",margin:"auto",display:"flex",flexDirection:"column",maxWidth: "1630px"}}>
              <h3>{t("views.compare.combined_systems")}</h3>
              <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
                <TimeAndDateSelector onChange={(tr,nowButton)=>internUpdateTimeRange(tr.time,tr.autoUpdate,nowButton)} timeRange={timeRange} timeRanges={durations}/>
              </div>
              <CheckBoxComponentFilters setCheckedSystemIds={setCheckedSystemIds} checkSystemIds={checkedSystemIds}
                                        systemIds={systems.data.map((v)=>v.id)} namings={systemMappings}
                                        getSystemColour={saveGetColorByName} />
              <div style={{display:"flex",alignContent:"center",marginTop:"15px"}}>
                <div style={{backgroundColor:"snow"}} className="fakeAccordion ">
                  <LineGraph deviceColours={getActiveColours()} valueNameOverrides={systemMappings} legendOverrideValue={t("components.graph_accordion.input_label_watt")} min={0}
                             timeRange={refTimeRange.current.time} graphData={graphData} unit="W"
                             labels={getLabels("InputWatt_")}
                             defaultDurations={systems.data.map(s=>s.defaultDuration)}/>
                </div>
                </div>
              <CombinedStatisticsAccordion activeSystemIds={checkedSystemIds} colors={getActiveColours()} systemNamings={systemMappings} systemInfos={systems.data}/>
            </div>
          </div>
          </>
          :
          <>
            <CircularProgress/> {t("common.loading.graph")}
          </>}
          </>:
          <>
            {t("views.compare.no_graphs")}
          </>}
        </>:
        <>:
        <CircularProgress/> {t("common.loading.system")}
      </>
    }

    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}


  </div>
}
