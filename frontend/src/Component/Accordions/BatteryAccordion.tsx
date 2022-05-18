import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {GraphDataObject} from "../DetailDashboard";
import {TimeAndDuration} from "../../context/time/TimeAndDateSelector";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  batteryVoltage?:number
  isBatteryPercentage?: boolean
  minBatteryVoltage?: number
  maxBatteryVoltage?: number
  timezone?  :string
}

export default function BatteryAccordion({timezone,timeRange,graphData,isBatteryPercentage,minBatteryVoltage,maxBatteryVoltage}: AccordionProps) {

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
          <LineGraph timezone={timezone} timeRange={timeRange} unit="W" graphData={graphData} labels={["BatteryWatt"]} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} min={minBatteryVoltage} max={maxBatteryVoltage} timeRange={timeRange} unit="V" graphData={graphData} labels={["BatteryVoltage"]} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} timeRange={timeRange} unit="A" graphData={graphData} labels={["ChargeAmpere"]} />
        </div>
        {isBatteryPercentage && <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} min={0} timeRange={timeRange} unit="%" graphData={graphData} labels={["BatteryPercentage"]} />
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
