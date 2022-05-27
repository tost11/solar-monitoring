import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../views/SystemDashboardView";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";

interface SolarPanelAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  maxSolarVoltage?: number
  onlyWatt?:boolean
  timezone: string
}

export default function SolarPanelAccordion({timezone,timeRange,graphData,maxSolarVoltage,onlyWatt}: SolarPanelAccordionProps) {

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
            <LineGraph timezone={timezone} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={["ChargeWatt"]} />
        </div>
        {!onlyWatt && <>
          <div className="defaultPanelWrapper">
              <LineGraph timezone={timezone} min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={["ChargeVolt"]} />
          </div>
          <div className="defaultPanelWrapper">
              <LineGraph timezone={timezone} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={["ChargeAmpere"]} />
          </div>
        </>}
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
