import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {GraphDataObject} from "../DetailDashboard";

interface AccordionProps {
  timeRange: string;
  graphData:GraphDataObject
}

export default function BatteryAccordion({timeRange,graphData}: AccordionProps) {



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
          <LineGraph timeRange={timeRange} graphData={graphData} labels={["BatteryWatt"]} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timeRange={timeRange} graphData={graphData} labels={["BatteryVoltage"]} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timeRange={timeRange} graphData={graphData} labels={["ChargeAmpere"]} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
