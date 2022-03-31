import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import ShowTimePickerComponent from "../ShowTimePickerComponent";
import {getStatisticGraphData} from "../../api/GraphAPI";
import BarGraph from "../BarGraph";
import {GraphDataObject} from "../DetailDashboard";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  consumption: boolean;
}

export default function StatisticsAccordion({systemInfo,consumption}: AccordionProps) {
  const [isOpen,setIsOpen] = useState(false)

  const generateDuration = (toTime:number,timeRange:string) => {
    let fromTime = toTime - 604800000 //default one week
    if (timeRange === "1M") {
      fromTime = toTime - 2674800000
    }
    if (timeRange === "1y"){
      fromTime  = toTime - 31532400000
    }
    return {fromTime,toTime,timeRange}
  }

  const [duration, setDuration] = useState(generateDuration(new Date().getTime(),"1w"));
  const [graphData,setGraphData] = useState<GraphDataObject>()

  const reloadData = ()=>{
    getStatisticGraphData(systemInfo.id, duration.fromTime,duration.toTime).then((r)=>{
      setGraphData({data:r})
    })
  }

  useEffect(()=>{
    reloadData()
  },[duration])

  const setAccordionStatus=(open:boolean)=>{
    if(open){
      reloadData()
    }else{
      setGraphData(undefined)
    }
    setIsOpen(open)
  }

  return <Accordion expanded={isOpen} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={(ev,open)=>setAccordionStatus(open)}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header">
      <Typography>Statistics</Typography>
    </AccordionSummary>

    <AccordionDetails>
      {graphData ? <div>
      <ShowTimePickerComponent creationDate={systemInfo.creationDate} setTimeRange={(s)=>setDuration(generateDuration(duration.toTime,s))} setSelectDate={(to:number)=>setDuration(generateDuration(to,duration.timeRange))}/>
        <div className="defaultFlowColumn">
          <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
            {consumption ? <div>
              <BarGraph from={duration.fromTime} to={duration.toTime} graphData={graphData} labels={["Produce","Consumption"]}/>
              <BarGraph  from={duration.fromTime} to={duration.toTime} graphData={graphData} labels={["Difference"]}/>
            </div>:
            <div>
              <BarGraph from={duration.fromTime} to={duration.toTime} graphData={graphData} labels={["Produce"]}/>
            </div>}
          </div>
        </div>
      </div>:<CircularProgress/>}
    </AccordionDetails>
  </Accordion>
}
