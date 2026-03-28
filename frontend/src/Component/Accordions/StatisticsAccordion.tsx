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
import {SolarSystemDTO} from "../../api/SolarSystemAPI";
import {getStatisticGraphData, getStatisticLastTwoDaysGraphData} from "../../api/GraphAPI";
import DayBarGraph, {BarGraphData} from "../DayBarGraph";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration, TimeRangeStatus} from "../time/TimeAndDateSelector";
import ContinuousUpdateWrapper from "../ContinuousUpdateWrapper";
import moment from "moment-timezone";
import {useTranslation} from "react-i18next";

interface AccordionProps {
  systemInfo: SolarSystemDTO;
}

export default function StatisticsAccordion({systemInfo}: AccordionProps) {

  const { t } = useTranslation()

  let startTimeRange  = generateTimeDuration("1w",moment())

  const [isOpen,setIsOpen] = useState(false)
  const refTimeRange = useRef({time:startTimeRange,autoUpdate:true})
  const [timeRange,setTimeRange] = useState<TimeRangeStatus>(refTimeRange.current)
  const [graphTimeRange,setGraphTimeRange] = useState(startTimeRange)
  const refGraphData = useRef<BarGraphData | undefined>({data:[]})
  const [graphData,setGraphData] = useState(refGraphData.current)
  const [consumptionEnabled,setConsumptionEnabled] = useState(true)
  const [productionEnabled,setProductionEnabled] = useState(true)
  const [gridFeedInEnabled, setGridFeedInEnabled] = useState(true)
  const [gridConsumedEnabled, setGridConsumedEnabled] = useState(true)

  let namings : {[key: string]: string} = {}
  namings["Consumed"] = t("components.graph_accordion.consumption")
  namings["Produced"] = t("components.graph_accordion.production")
  namings["Difference"] = t("components.graph_accordion.difference")
  namings["Battery"] = t("components.graph_accordion.battery")
  namings["GridFeedIn"] = t("components.graph_accordion.grid_feedin")
  namings["GridConsumed"] = t("components.graph_accordion.grid_consumption")

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
      r = await getStatisticGraphData(systemInfo.id, tr.start.valueOf(), tr.end.valueOf());
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
    let res: []
    try {
      res = await getStatisticLastTwoDaysGraphData(systemInfo.id);
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

  const renderConsumption = ()=>{
    return (!systemInfo.viewData.hideTotalConsumption || !systemInfo.viewData.hideTotalConsumption) && !systemInfo.publicFlagOnlyProduction && systemInfo.type != "VERY_SIMPLE" && systemInfo.type != "SIMPLE";
  }

  const renderBattery = ()=>{
    return !systemInfo.publicFlagOnlyProduction && (systemInfo.type == "SELFMADE" || systemInfo.type == "GRID_BATTERY");
  }

  const renderGrid = () => {
    return !systemInfo.publicFlagOnlyProduction && (systemInfo.type == "GRID_BATTERY");
  }

  const getGridLabels = () => {
    let arr = [];
    if(gridFeedInEnabled) arr.push("GridFeedIn")
    if(gridConsumedEnabled) arr.push("GridConsumed")
    return arr;
  }

  const getGridColors = () => {
    let arr = [];
    if(gridFeedInEnabled) arr.push('#FF8C00')
    if(gridConsumedEnabled) arr.push('#8B008B')
    return arr;
  }

  const colors = ['#089c19','rgb(234,6,6)','darkblue']

  return <div style={{marginTop: "5px"}}>
    <Accordion expanded={isOpen} style={{backgroundColor:"snow"}} className={"DetailAccordion"} onChange={(ev,open)=>setAccordionStatus(open)}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        style={{backgroundColor:"steelblue"}}
    >
      <Typography>{t("components.graph_accordion.history")}</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <ContinuousUpdateWrapper fullReloadCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,true)}
                               updateCallback={()=>internalSetTimeRange(generateTimeDuration(refTimeRange.current.time.durationString,moment()),true,false)}
                               fetchTimout={1000 * 60 * 10} fullReloadTimeout={1000 * 60 * 60} active={isOpen && timeRange.autoUpdate}/>
      {graphData ? <div>
        <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
          <TimeAndDateSelector minDate={moment(systemInfo.buildingDate)} onlyDate={true} onChange={(time,nowButton)=>internalSetTimeRange(time.time,time.autoUpdate,nowButton)}
                               timeRange={timeRange} timezone={systemInfo.timezone} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>
        </div>
        <div className="defaultFlowColumn">
          <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
            {renderConsumption() ? <div>
              <FormControlLabel
                label={<div style={{color:colors[0]}}>{t("components.graph_accordion.production")}</div>}
                control={<Checkbox
                  checked={productionEnabled}
                  onChange={()=>setProductionEnabled(!productionEnabled)}
                  inputProps={{ 'aria-label': 'controlled' }}
                />}
              />

              <FormControlLabel
                label={<div style={{color:colors[1]}}>{t("components.graph_accordion.consumption")}</div>}
                control={<Checkbox
                  checked={consumptionEnabled}
                  onChange={()=>setConsumptionEnabled(!consumptionEnabled)}
                  inputProps={{ 'aria-label': 'controlled' }}
                />}
              />

              <DayBarGraph
                multFactor={1000}
                timezone = {systemInfo.timezone}
                unit="Wh" timeRange={graphTimeRange}
                graphData={graphData}
                labels={getActiveLabels()}
                colors={getActiveColors()}
                valueNameOverrides={namings}
              />
              <DayBarGraph
                multFactor={1000}
                timezone = {systemInfo.timezone}
                unit="wh" timeRange={graphTimeRange}
                graphData={graphData}
                labels={["Difference"]}
                colors={[colors[0]]}
                negativeColours={[colors[1]]}
                valueNameOverrides={namings}
              />
            </div>:
            <div>
              <DayBarGraph
                multFactor={1000}
                timezone = {systemInfo.timezone}
                unit="wh" timeRange={graphTimeRange}
                graphData={graphData}
                labels={["Produced"]}
                valueNameOverrides={namings}
              />
            </div>}
            {renderBattery() &&
              <DayBarGraph
                multFactor={1000}
                timezone={systemInfo.timezone}
                unit="wh" timeRange={graphTimeRange}
                graphData={graphData}
                labels={["Battery"]}
                colors={[colors[2]]}
                negativeColours={[colors[1]]}
                valueNameOverrides={namings}
              />
            }
            {renderGrid() && getGridLabels().length > 0 &&
              <div style={{marginTop:"20px"}}>
                <FormControlLabel
                  label={<div style={{color:'#FF8C00'}}>{t("components.graph_accordion.grid_feedin")}</div>}
                  control={<Checkbox
                    checked={gridFeedInEnabled}
                    onChange={()=>setGridFeedInEnabled(!gridFeedInEnabled)}
                    inputProps={{ 'aria-label': 'controlled' }}
                  />}
                />

                <FormControlLabel
                  label={<div style={{color:'#8B008B'}}>{t("components.graph_accordion.grid_consumption")}</div>}
                  control={<Checkbox
                    checked={gridConsumedEnabled}
                    onChange={()=>setGridConsumedEnabled(!gridConsumedEnabled)}
                    inputProps={{ 'aria-label': 'controlled' }}
                  />}
                />

                <DayBarGraph
                  multFactor={1000}
                  timezone={systemInfo.timezone}
                  unit="Wh"
                  timeRange={graphTimeRange}
                  graphData={graphData}
                  labels={getGridLabels()}
                  colors={getGridColors()}
                  valueNameOverrides={namings}
                />
              </div>
            }
          </div>
        </div>
        </div>:<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
