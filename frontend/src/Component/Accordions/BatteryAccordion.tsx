import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import LineGraph from "../LineGraph";
import {GraphDataObject} from "../DetailDashboard";

interface AccordionProps {
  timeRange: string;
  graphData?:GraphDataObject
  labels:string[string[]]
}

export default function BatteryAccordion({timeRange,graphData,labels}: AccordionProps) {



  return <div>{graphData&&
  <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Battery</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
          {labels.map((value,index)=>{
            return <LineGraph key={index} timeRange={timeRange} graphData={graphData} labels={value}/>
          })}
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
