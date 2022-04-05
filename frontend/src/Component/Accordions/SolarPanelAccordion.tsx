import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../../context/time/TimeAndDateSelector";

interface SolarPanelAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  maxSolarVoltage?: number
}

export default function SolarPanelAccordion({timeRange,graphData,maxSolarVoltage}: SolarPanelAccordionProps) {

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
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={["ChargeWatt"]} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={["ChargeVolt"]} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={["ChargeAmpere"]} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
