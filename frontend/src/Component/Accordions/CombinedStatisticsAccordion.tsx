import React, {useRef, useState} from "react";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  CircularProgress,
  Typography
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {MultSolarSystemDTO, SolarSystemDTO} from "../../api/SolarSystemAPI";
import {
  getCombinedStatisticGraphData, getCombinedStatisticLastTwoDaysGraphData
} from "../../api/GraphAPI";
import DayBarGraph, {BarGraphData} from "../DayBarGraph";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration, TimeRangeStatus} from "../time/TimeAndDateSelector";
import ContinuousUpdateWrapper from "../ContinuousUpdateWrapper";
import moment from "moment-timezone";
import {useTranslation} from "react-i18next";

interface AccordionProps {
  systemInfos: MultSolarSystemDTO[],
  systemNamings: {[key: string]: string},
  colors?: string[],
  activeSystemIds?:Set<string>
}

export default function CombinedStatisticsAccordion({systemInfos,systemNamings,colors,activeSystemIds}: AccordionProps) {

  const { t } = useTranslation()

  let startTimeRange  = generateTimeDuration("1w",moment())

  const [isOpen,setIsOpen] = useState(false)
  const refTimeRange = useRef({time:startTimeRange,autoUpdate:true})
  const [timeRange,setTimeRange] = useState<TimeRangeStatus>(refTimeRange.current)
  const [graphTimeRange,setGraphTimeRange] = useState(startTimeRange)
  const refGraphData = useRef<BarGraphData | undefined>({data:[]})
  const [graphData,setGraphData] = useState(refGraphData.current)

  const getLabels = (startPattern:string)=>{
    let ret = [];
    for (let i = 0; i < systemInfos.length; i++) {
      if(!activeSystemIds || activeSystemIds.has(systemInfos[i].id)) {
        ret.push(startPattern + i);
      }
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
      return false
    }

    refGraphData.current = {data: r}
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

  return <div className={"fakeAccordion"} style={{marginTop: "5px",backgroundColor:"transparent",padding:"0px"}}>
    <Accordion style={{backgroundColor:"snow"}} expanded={isOpen} onChange={(ev,open)=>setAccordionStatus(open)}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        style={{backgroundColor:"steelblue"}}
      >
      <Typography>Statistics</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <ContinuousUpdateWrapper fullReloadCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
                               updateCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
                               fetchTimout={1000 * 60 * 10} fullReloadTimeout={1000 * 60 * 60} active={isOpen && timeRange.autoUpdate}/>
      {graphData ? <div>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <TimeAndDateSelector onlyDate={true} onChange={(time,nowButton)=>internalSetTimeRange(time.time,time.autoUpdate,nowButton)}
                               timeRange={timeRange} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>
        </div>
        <div style={{marginTop:"15px"}}>
          <DayBarGraph
            multFactor={1000}
            timezone={"UTC"}
            unit="Wh" timeRange={graphTimeRange}
            graphData={graphData}
            colors={colors}
            labels={getLabels("Produced_")}
            valueNameOverrides={systemNamings}
          />
        </div>
        </div> :<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
