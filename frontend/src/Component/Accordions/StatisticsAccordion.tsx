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
import {getStatisticGraphData} from "../../api/GraphAPI";
import moment from "moment";
import BarGraph from "../BarGraph";
import TimeAndDateSelector, {generateTimeDuration} from "../time/TimeAndDateSelector";
import {GraphDataObject} from "../../views/SystemDashboardView";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  consumption: boolean;
}

export default function StatisticsAccordion({systemInfo,consumption}: AccordionProps) {

  const [isOpen,setIsOpen] = useState(false)
  const [timeRange,setTimeRange] = useState(generateTimeDuration("1w",new Date()))
  const [graphData,setGraphData] = useState<GraphDataObject>()
  const [consumptionEnabled,setConsumptionEnabled] = useState(true)
  const [productionEnabled,setProductionEnabled] = useState(true)

  const reloadData = ()=>{
    getStatisticGraphData(systemInfo.id,systemInfo.type, timeRange.start.getTime(),timeRange.end.getTime()).then((r)=>{
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

  const colors = ['#089c19','rgb(234,6,6)']

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
          <TimeAndDateSelector minDate={systemInfo.buildingDate} onlyDate={true} maxDate={new Date()} onChange={setTimeRange} timeRange={timeRange} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>
        </div>
         <div className="defaultFlowColumn">
            <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
              {consumption ? <div>

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
                  timezone = {systemInfo.timezone}
                  unit="Wh" timeRange={timeRange}
                  graphData={graphData}
                  labels={getActiveLabels()}
                  colors={getActiveColors()}
                />
                <BarGraph
                  timezone = {systemInfo.timezone}
                  unit="Wh" timeRange={timeRange}
                  graphData={graphData}
                  labels={["Difference"]}
                  colors={[colors[0]]}
                  negativeColours={[colors[1]]}
                />
              </div>:
              <div>
                <BarGraph
                  timezone = {systemInfo.timezone}
                  unit="Wh" timeRange={timeRange}
                  graphData={graphData}
                  labels={["Produced"]}/>
              </div>}
            </div>
          </div>
        </div>:<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
