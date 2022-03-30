import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";

interface SolarPanelAccordionProps {
  timeRange: string;
  graphData:GraphDataObject
}


export default function SolarPanelAccordion({timeRange,graphData}: SolarPanelAccordionProps) {

  return<div>{graphData&&
 <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
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
            <LineGraph timeRange={timeRange} graphData={graphData} labels={["ChargeWatt"]} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timeRange={timeRange} graphData={graphData} labels={["ChargeVolt"]} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timeRange={timeRange} graphData={graphData} labels={["ChargeAmpere"]} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
