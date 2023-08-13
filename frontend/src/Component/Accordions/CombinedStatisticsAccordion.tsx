import React, {useEffect, useRef, useState} from "react";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary, Checkbox,
  CircularProgress,
  FormControlLabel,
  Typography
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {MultSolarSystemDTO, SolarSystemDTO} from "../../api/SolarSystemAPI";
import {
  getCombinedStatisticGraphData, getCombinedStatisticLastTwoDaysGraphData,
  getStatisticGraphData,
  getStatisticLastTwoDaysGraphData
} from "../../api/GraphAPI";
import DayBarGraph, {BarGraphData} from "../DayBarGraph";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration, TimeRangeStatus} from "../time/TimeAndDateSelector";
import ContinuousUpdateWrapper from "../ContinuousUpdateWrapper";
import moment from "moment-timezone";

interface AccordionProps {
  systemInfos: MultSolarSystemDTO[];
  systemNamings: {[key: string]: string};
}

export default function CombinedStatisticsAccordion({systemInfos,systemNamings}: AccordionProps) {
  let startTimeRange  = generateTimeDuration("1w",moment())

  const [isOpen,setIsOpen] = useState(false)
  const refTimeRange = useRef({time:startTimeRange,autoUpdate:true})
  const [timeRange,setTimeRange] = useState<TimeRangeStatus>(refTimeRange.current)
  const [graphTimeRange,setGraphTimeRange] = useState(startTimeRange)
  const refGraphData = useRef<BarGraphData | undefined>({data:[]})
  const [graphData,setGraphData] = useState(refGraphData.current)
  const [consumptionEnabled,setConsumptionEnabled] = useState(true)
  const [productionEnabled,setProductionEnabled] = useState(true)

  const getLabels = (startPattern:string)=>{
    let ret = [];
    for (let i = 0; i < systemInfos.length; i++) {
      ret.push(startPattern + i);
    }
    return ret;
  }

  const internalSetTimeRange = async (newTimeRange:TimeAndDuration,autoUpdate: boolean,forceFullReload:boolean) => {

    let fullFetch = forceFullReload || autoUpdate == false || (refTimeRange.current.autoUpdate == false && autoUpdate == true) || newTimeRange.duration != refTimeRange.current.time.duration
    let toUse = {
      start: moment(newTimeRange.start),
      end: moment(newTimeRange.end),
      duration: newTimeRange.duration,
      durationString: newTimeRange.durationString
    }

    refTimeRange.current = {time:newTimeRange,autoUpdate: autoUpdate};
    setTimeRange(refTimeRange.current)
    setGraphTimeRange(toUse)

    if(fullFetch){
      setGraphData({data:[]})
      return reloadData(newTimeRange)
    }else{
      return reloadLastTwoDays(newTimeRange)
    }
  }

  const reloadData = async (tr:TimeAndDuration) => {
    let r: []
    try {
      r = await getCombinedStatisticGraphData(systemInfos.map(s=>s.id), tr.start.valueOf(), tr.end.valueOf());
    } catch (e) {
      console.log(e)
      return false
    }

    /*let all = getAllTicks();

    r.forEach(r=>{
      all.delete(r.time);
    })

    all.forEach(t=>{
      r.push({"time":t})
    })

    //TODO find better way to to this
    r.sort((v1,v2)=>v1.time<v2.time?1:0);

    //console.log(r)*/

    refGraphData.current = {data: r}

    //console.log('start ' + now.startOf('day').toString())

    setGraphData(refGraphData.current)

    return true;
  }

  const reloadLastTwoDays = async (tr:TimeAndDuration) => {
    //TODO implement
    return true;
    let res: []
    try {
      res = await getCombinedStatisticLastTwoDaysGraphData(systemInfos.map(s=>s.id));
    } catch (e) {
      return false;
    }

    // @ts-ignore
    let newData: any[] = []

    refGraphData.current?.data.forEach(d => {
      // @ts-ignore
      if (d.time > tr.start.valueOf() && res.filter(e => e.time === d.time).length == 0) {
        newData.push(d)
      }
    })
    res.forEach(d => {
      newData.push(d)
    })

    refGraphData.current = {data: newData}
    setGraphData(refGraphData.current)

    return true;
  }

  const setAccordionStatus=(open:boolean)=>{
    if(open){
      reloadData(refTimeRange.current.time)
    }else{
      refGraphData.current = undefined;
      setGraphData(undefined)
    }
    setIsOpen(open)
  }

  const getActiveLabels = () =>{
    let arr = [];
    if(consumptionEnabled){
      arr.push("Consumed")
    }
    if(productionEnabled){
      arr.push("Produced")
    }
    return arr;
  }

  const getActiveColors = () =>{
    let arr = [];
    if(consumptionEnabled){
      arr.push(colors[1])
    }
    if(productionEnabled){
      arr.push(colors[0])
    }
    return arr;
  }

  /*
  const renderConsumption = ()=>{
    return !systemInfo.publicFlagOnlyProduction && systemInfo.type != "VERY_SIMPLE" && systemInfo.type != "SIMPLE";
  }

  const renderBattery = ()=>{
    return !systemInfo.publicFlagOnlyProduction && (systemInfo.type == "SELFMADE" || systemInfo.type == "GRID_BATTERY");
  }*/

  const colors = ['#089c19','rgb(234,6,6)','darkblue']

  return <div className={"fakeAccordion"} style={{marginTop: "5px",backgroundColor:"transparent",padding:"0px"}}>
    <Accordion expanded={isOpen} style={{backgroundColor:"Lavender"}} onChange={(ev,open)=>setAccordionStatus(open)}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header">
      <Typography>Statistics</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <ContinuousUpdateWrapper fullReloadCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
                               updateCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
                               fetchTimout={/*1000 * 60 * 10*/10000} fullReloadTimeout={1000 * 60 * 60} active={isOpen && timeRange.autoUpdate}/>
      {graphData ? <div>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <TimeAndDateSelector onlyDate={true} onChange={(time,nowButton)=>internalSetTimeRange(time.time,time.autoUpdate,nowButton)}
                               timeRange={timeRange} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>
          <div style={{marginTop:"auto",marginBottom:"auto",marginRight:"10px", marginLeft:"20px"}}>
            Update: {timeRange.autoUpdate ? "on":"off"}
          </div>
        </div>

            <DayBarGraph
              multFactor={1000}
              timezone={"UTC"}
              unit="Wh" timeRange={graphTimeRange}
              graphData={graphData}
              labels={getLabels("Produced_")}
              valueNameOverrides={systemNamings}
            />
            {/*<DayBarGraph
              multFactor={1000}
              unit="wh" timeRange={graphTimeRange}
              graphData={graphData}
              labels={["Difference"]}
              colors={[colors[0]]}
              negativeColours={[colors[1]]}
            />*/}
            {/*renderBattery() &&
              <DayBarGraph
                multFactor={1000}
                timezone={systemInfo.timezone}
                unit="wh" timeRange={graphTimeRange}
                graphData={graphData}
                labels={["Battery"]}
                colors={[colors[2]]}
                negativeColours={[colors[1]]}
              />
            */}
        </div>:<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
