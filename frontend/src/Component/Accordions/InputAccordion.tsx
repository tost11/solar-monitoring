import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../views/SystemDashboardView";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";

interface GridInputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  maxSolarVoltage?: number
  deviceIds:Set<number>
  inputIds:Set<number>
  showCombined: boolean
  getDeviceColour: (name:string)=>string
  timezone: string
}

export default function InputAccordion({timezone,timeRange,graphData,maxSolarVoltage,getDeviceColour,showCombined,inputIds,deviceIds}: GridInputAccordionProps) {

  let colors = [];
  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
  }

  const wattLabels = showCombined ? ["InputWatt"] : [];
  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d));
    wattLabels.push("InputWatt"+"-d-"+d)
  })
  inputIds?.forEach(d=>{
    colors.push(getDeviceColour("i-"+d));
    wattLabels.push("Watt"+"-i-"+d)
  })

  const voltLabels = showCombined ? ["InputVoltage"] : [];
  deviceIds?.forEach(d=>voltLabels.push("InputVoltage"+"-d-"+d))
  inputIds?.forEach(d=>voltLabels.push("Voltage"+"-i-"+d))

  const ampereLabels = showCombined ? ["InputAmpere"] : [];
  deviceIds?.forEach(d=>ampereLabels.push("InputAmpere"+"-d-"+d))
  inputIds?.forEach(d=>ampereLabels.push("Ampere"+"-i-"+d))

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
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input in Watt"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Voltage"} min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input in Ampere"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
