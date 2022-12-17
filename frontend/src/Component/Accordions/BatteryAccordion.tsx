import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {GraphDataObject} from "../../views/SystemDashboardView";
import {TimeAndDuration} from "../time/TimeAndDateSelector";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  batteryVoltage?:number
  deviceIds:Set<number>
  batteryIds:Set<number>
  isBatteryPercentage?: boolean
  minBatteryVoltage?: number
  maxBatteryVoltage?: number
  timezone?  :string,
  showCombined :boolean
}

export default function BatteryAccordion({timezone,timeRange,graphData,isBatteryPercentage,minBatteryVoltage,maxBatteryVoltage,deviceIds,batteryIds,showCombined}: AccordionProps) {

  const wattLabels = showCombined ? ["BatteryWatt"] : [];
  deviceIds?.forEach(d=>wattLabels.push("BatteryWatt"+"-d-"+d))
  batteryIds?.forEach(d=>wattLabels.push("Watt"+"-o-"+d))

  const voltLabels = showCombined ? ["BatteryVoltage"] : [];
  deviceIds?.forEach(d=>voltLabels.push("BatteryVoltage"+"-d-"+d))
  batteryIds?.forEach(d=>voltLabels.push("Voltage"+"-b-"+d))

  const ampereLabels = showCombined ? ["OutputAmpere"] : [];
  deviceIds?.forEach(d=>ampereLabels.push("OutputAmpere"+"-d-"+d))
  batteryIds?.forEach(d=>ampereLabels.push("Ampere"+"-b-"+d))

  return <div>{graphData &&
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
          <LineGraph timezone={timezone} timeRange={timeRange} unit="W" graphData={graphData} labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} min={minBatteryVoltage} max={maxBatteryVoltage} timeRange={timeRange} unit="V" graphData={graphData} labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} timeRange={timeRange} unit="A" graphData={graphData} labels={ampereLabels} />
        </div>
        {isBatteryPercentage && <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} min={0} timeRange={timeRange} unit="%" graphData={graphData} labels={["BatteryPercentage"]} />
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
