import React, {useEffect, useState} from "react";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary, Checkbox,
  CircularProgress,
  FormControlLabel,
  Typography
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import {getStatisticGraphData, GraphDataObject} from "../../api/GraphAPI";
import moment from "moment";
import BarGraph from "../BarGraph";
import TimeAndDateSelector, {generateTimeDuration, TimeAndDuration} from "../time/TimeAndDateSelector";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
}

export default function StatisticsAccordion({systemInfo}: AccordionProps) {

  let startDate = new Date()
  startDate.setDate(startDate.getDate() + 1)
  startDate.setHours(0)
  startDate.setMinutes(0)
  startDate.setSeconds(0)
  startDate.setMilliseconds(0)

  const [isOpen,setIsOpen] = useState(false)
  const [timeRange,setTimeRange] = useState(generateTimeDuration("1w",new Date()))
  const [graphTimeRange,setGraphTimeRange] = useState(generateTimeDuration("1w",startDate))
  const [graphData,setGraphData] = useState<{data:[]}>()
  const [consumptionEnabled,setConsumptionEnabled] = useState(true)
  const [productionEnabled,setProductionEnabled] = useState(true)

  const internalSetTimeRange = (timeRange:TimeAndDuration) => {
    let toUse = {
      start: new Date(timeRange.start),
      end: new Date(timeRange.end),
      duration: timeRange.duration,
      durationString: timeRange.durationString
    }

    toUse.start.setDate(toUse.start.getDate())
    toUse.start.setHours(12)
    toUse.start.setMinutes(0)
    toUse.start.setSeconds(0)
    toUse.start.setMilliseconds(0)

    toUse.end.setDate(toUse.end.getDate())
    toUse.end.setHours(12)
    toUse.end.setMinutes(0)
    toUse.end.setSeconds(0)
    toUse.end.setMilliseconds(0)

    setTimeRange(timeRange)
    setGraphTimeRange(toUse)
  }

  const reloadData = ()=>{
    getStatisticGraphData(systemInfo.id, timeRange.start.getTime(), timeRange.end.getTime()).then((r)=>{
      setGraphData({data:r})
    })
  }

  useEffect(()=>{
    if(isOpen) {//on initial load this here is needet i have no clue why
      reloadData()
    }
  },[timeRange])

  const formatDate = (date:any) => {
    if(!date){
      return undefined;
    }
    return moment(date).format('YYYY-MM-DD')
  }

  const setAccordionStatus=(open:boolean)=>{
    if(open){
      reloadData()
    }else{
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
    return systemInfo.type != "VERY_SIMPLE" && systemInfo.type != "SIMPLE";
  }

  const renderBattery = ()=>{
    return systemInfo.type == "SELFMADE" || systemInfo.type == "GRID_BATTERY";
  }

  const colors = ['#089c19','rgb(234,6,6)','darkblue']

  return <div style={{marginTop: "5px"}}>
    <Accordion expanded={isOpen} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={(ev,open)=>setAccordionStatus(open)}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header">
      <Typography>Statistics</Typography>
    </AccordionSummary>
    <AccordionDetails>
      {graphData ? <div>
        <div>
          <TimeAndDateSelector minDate={systemInfo.buildingDate} onlyDate={true} maxDate={new Date()} onChange={internalSetTimeRange} timeRange={timeRange} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>
        </div>
         <div className="defaultFlowColumn">
            <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
              {renderConsumption() ? <div>

                  <FormControlLabel
                    label={<div style={{color:colors[0]}}>Production</div>}
                    control={<Checkbox
                      checked={productionEnabled}
                      onChange={()=>setProductionEnabled(!productionEnabled)}
                      inputProps={{ 'aria-label': 'controlled' }}
                    />}
                  />

                <FormControlLabel
                  label={<div style={{color:colors[1]}}>Consumption</div>}
                  control={<Checkbox
                    checked={consumptionEnabled}
                    onChange={()=>setConsumptionEnabled(!consumptionEnabled)}
                    inputProps={{ 'aria-label': 'controlled' }}
                  />}
                />

                <BarGraph
                  multFactor={1000}
                  timezone = {systemInfo.timezone}
                  unit="Wh" timeRange={graphTimeRange}
                  graphData={graphData}
                  labels={getActiveLabels()}
                  colors={getActiveColors()}
                />
                <BarGraph
                  multFactor={1000}
                  timezone = {systemInfo.timezone}
                  unit="wh" timeRange={graphTimeRange}
                  graphData={graphData}
                  labels={["Difference"]}
                  colors={[colors[0]]}
                  negativeColours={[colors[1]]}
                />
              </div>:
              <div>
                <BarGraph
                  multFactor={1000}
                  timezone = {systemInfo.timezone}
                  unit="wh" timeRange={graphTimeRange}
                  graphData={graphData}
                  labels={["Produced"]}/>
              </div>}
              {renderBattery() &&
                  <BarGraph
                      multFactor={1000}
                      timezone={systemInfo.timezone}
                      unit="wh" timeRange={graphTimeRange}
                      graphData={graphData}
                      labels={["Battery"]}
                      colors={[colors[2]]}
                      negativeColours={[colors[1]]}
                  />
              }
            </div>
          </div>
        </div>:<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
