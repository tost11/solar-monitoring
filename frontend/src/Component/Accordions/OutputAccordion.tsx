import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {GraphDataObject} from "../../views/SystemDashboardView";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";

interface GridOutputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  deviceIds:Set<string>
  outputIds:Set<string>
  showCombined: boolean,
  timezone?  :string,
  getDeviceColour: (name:string)=>string,
  systemType: string
}

export default function OutputAccordion({timezone,timeRange,graphData,outputIds,deviceIds,showCombined,getDeviceColour,systemType}: GridOutputAccordionProps) {

  let colors = [];
  let wattLabels:string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabels.push("OutputWatt")
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d));
    wattLabels.push("OutputWatt"+"-d-"+d)
  })
  outputIds?.forEach(d=>{
    colors.push(getDeviceColour("o-"+d));
    wattLabels.push("Watt"+"-o-"+d)
  })

  const voltLabels = showCombined ? ["OutputVoltage"] : [];
  deviceIds?.forEach(d=>voltLabels.push("OutputVoltage"+"-d-"+d))
  outputIds?.forEach(d=>voltLabels.push("Voltage"+"-o-"+d))

  const ampereLabels = showCombined ? ["OutputAmpere"] : [];
  deviceIds?.forEach(d=>ampereLabels.push("OutputAmpere"+"-d-"+d))
  outputIds?.forEach(d=>ampereLabels.push("Ampere"+"-o-"+d))

  const frequencyLabels = showCombined ? ["OutputFrequency"] : [];
  deviceIds?.forEach(d=>frequencyLabels.push("OutputFrequency"+"-d-"+d))
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
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Power in Watt"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Voltage"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Power in Ampere"}  min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
        {(systemType == "GRID" || systemType == "GRID_BATTERY" || systemType == "SELFMADE_INVERTER" || systemType == "SELFMADE_CONSUMPTION") &&
            <div className="defaultPanelWrapper">
              <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Frequency"}
                         timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
            </div>
        }
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
