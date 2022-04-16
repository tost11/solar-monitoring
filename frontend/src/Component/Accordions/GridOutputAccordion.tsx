import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../../context/time/TimeAndDateSelector";

interface GridOutputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  gridVoltage?: number,
  deviceIds:Set<number>
  showCombined: boolean,
  deviceColours: string[]
}

export default function GridOutputAccordion({timeRange,graphData,gridVoltage,deviceIds,showCombined,deviceColours}: GridOutputAccordionProps) {

  const wattLabel = "ChargeWatt";
  const voltageLabel = "ChargeVoltage";
  const ampereLabel = "ChargeAmpere";
  const frequencyLabel = "Frequency";

  const wattLabels = showCombined ? [wattLabel] : [];
  deviceIds?.forEach(d=>wattLabels.push(wattLabel+"_"+d))

  const voltLabels = showCombined ? [voltageLabel] : [];
  deviceIds?.forEach(d=>voltLabels.push(voltageLabel+"_"+d))

  const ampereLabels = showCombined ? [ampereLabel] : [];
  deviceIds?.forEach(d=>ampereLabels.push(ampereLabel+"_"+d))

  const frequencyLabels = showCombined ? ["Frequency"] : [];
  deviceIds?.forEach(d=>frequencyLabels.push("Frequency_"+d))

return<div>{graphData&&
 <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Output</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
            <LineGraph deviceColours={deviceColours} legendOverrideValue={wattLabel} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph deviceColours={deviceColours} legendOverrideValue={voltageLabel}  min={gridVoltage?gridVoltage-5:undefined} max={gridVoltage?gridVoltage+5:undefined} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph deviceColours={deviceColours} legendOverrideValue={ampereLabel}  min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph deviceColours={deviceColours} legendOverrideValue={frequencyLabel}  timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
