import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import {getStatisticGraphData} from "../../api/GraphAPI";
import moment from "moment";
import BarGraph from "../BarGraph";
import TimeAndDateSelector, {generateTimeDuration} from "../../context/time/TimeAndDateSelector";
import {GraphDataObject} from "../DetailDashboard";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  consumption: boolean;
}

export default function StatisticsAccordion({systemInfo,consumption}: AccordionProps) {
  const [isOpen,setIsOpen] = useState(false)

  const generateDuration = (toTime:number,timeRange:string) => {
    let fromTime = toTime - 200 //default one week
    return {fromTime,toTime,timeRange}
  }

  const [timeRange,setTimeRange] = useState(generateTimeDuration("1w",new Date()))

  const [graphData,setGraphData] = useState<GraphDataObject>()

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
                <BarGraph unit="Wh" timeRange={timeRange} graphData={graphData} labels={["Produce","Consumption"]}/>
                <BarGraph unit="Wh" timeRange={timeRange} graphData={graphData} labels={["Difference"]}/>
              </div>:
              <div>
                <BarGraph unit="Wh" timeRange={timeRange} graphData={graphData} labels={["Produce"]}/>
              </div>}
            </div>
          </div>
        </div>:<CircularProgress/>}
      </AccordionDetails>
    </Accordion>
  </div>
}
