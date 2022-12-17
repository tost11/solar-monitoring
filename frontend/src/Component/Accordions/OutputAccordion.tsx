import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../views/SystemDashboardView";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";

interface GridOutputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  deviceIds:Set<number>
  outputIds:Set<number>
  showCombined: boolean,
  deviceColours: string[],
  timezone?  :string
}

export default function OutputAccordion({timezone,timeRange,graphData,outputIds,deviceIds,showCombined,deviceColours}: GridOutputAccordionProps) {

  const wattLabels = showCombined ? ["OutputWatt"] : [];
  deviceIds?.forEach(d=>wattLabels.push("OutputWatt"+"-d-"+d))
  outputIds?.forEach(d=>wattLabels.push("Watt"+"-o-"+d))

  const voltLabels = showCombined ? ["OutputVoltage"] : [];
  deviceIds?.forEach(d=>voltLabels.push("OutputVoltage"+"-d-"+d))
  outputIds?.forEach(d=>voltLabels.push("Voltage"+"-o-"+d))

  const ampereLabels = showCombined ? ["OutputAmpere"] : [];
  deviceIds?.forEach(d=>ampereLabels.push("OutputAmpere"+"-d-"+d))
  outputIds?.forEach(d=>ampereLabels.push("Ampere"+"-o-"+d))

  const frequencyLabels = showCombined ? ["Frequency"] : [];
  deviceIds?.forEach(d=>frequencyLabels.push("Frequency"+"-d-"+d))
  outputIds?.forEach(d=>frequencyLabels.push("Frequency"+"-o-"+d))


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
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={"Watt"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={"Voltage"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={"Ampere"}  min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={deviceColours} legendOverrideValue={"Frequency"}  timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
