import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../../context/time/TimeAndDateSelector";

interface GridInputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  maxSolarVoltage?: number
  deviceIds:Set<number>
  showCombined: boolean
  deviceColours: string[]
  timezone: string
}

export default function GridInputAccordion({timezone,timeRange,graphData,maxSolarVoltage,deviceIds,showCombined,deviceColours}: GridInputAccordionProps) {

  const wattLabel = "ChargeWatt";
  const voltageLabel = "ChargeVoltage";
  const ampereLabel = "ChargeAmpere";

  const wattLabels = showCombined ? [wattLabel] : [];
  deviceIds?.forEach(d=>wattLabels.push(wattLabel+"_"+d))

  const voltLabels = showCombined ? [voltageLabel] : [];
  deviceIds?.forEach(d=>voltLabels.push(voltageLabel+"_"+d))

  const ampereLabels = showCombined ? [ampereLabel] : [];
  deviceIds?.forEach(d=>ampereLabels.push(ampereLabel+"_"+d))

return<div>{graphData&&
 <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Input</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={wattLabel} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={voltageLabel} min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={ampereLabel} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
