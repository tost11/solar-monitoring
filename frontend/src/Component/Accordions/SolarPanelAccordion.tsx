import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import Graph from "../Graph";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  dashboardPath: String;
  refresh: string;
  timeRange: string;
}


export default function SolarPanelAccordion({timeRange,refresh,systemInfo, dashboardPath}: AccordionProps) {
  const [isOpen, setIsOpen] = useState(false)


  const changePanelStatus=()=>{
    setIsOpen(!isOpen)
  }

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Solar</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
          <Graph timeRange={timeRange} systemInfo={systemInfo}/>
          <Graph timeRange={timeRange} systemInfo={systemInfo}/>
        </div>
      </div>
    </AccordionDetails>
  </Accordion>
}
