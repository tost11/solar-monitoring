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
  outputDCIds:Set<string>
  outputACIds:Set<string>
  showCombined: boolean,
  timezone?  :string,
  getDeviceColour: (name:string)=>string,
  systemType: string
}

export default function OutputAccordion({timezone,timeRange,graphData,outputDCIds,outputACIds,deviceIds,showCombined,getDeviceColour,systemType}: GridOutputAccordionProps) {

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
  outputDCIds?.forEach(d=>{
    colors.push(getDeviceColour("o-"+d));
    wattLabels.push("Watt"+"-o-"+d)
  })
  outputACIds?.forEach(d=>{
    colors.push(getDeviceColour("c-"+d));
    wattLabels.push("Watt"+"-c-"+d)
  })

  const voltLabelsDC = showCombined ? ["OutputVoltageDC"] : [];
  deviceIds?.forEach(d=>voltLabelsDC.push("OutputVoltageDC"+"-d-"+d))
  outputDCIds?.forEach(d=>voltLabelsDC.push("Voltage"+"-o-"+d))

  const ampereLabelsDC = showCombined ? ["OutputAmpereDC"] : [];
  deviceIds?.forEach(d=>ampereLabelsDC.push("OutputAmpereDC"+"-d-"+d))
  outputDCIds?.forEach(d=>ampereLabelsDC.push("Ampere"+"-o-"+d))

  const voltLabelsAC = showCombined ? ["OutputVoltageAC"] : [];
  deviceIds?.forEach(d=>voltLabelsAC.push("OutputVoltageAC"+"-d-"+d))
  outputACIds?.forEach(d=>voltLabelsAC.push("Voltage"+"-c-"+d))

  const ampereLabelsAC= showCombined ? ["OutputAmpereAC"] : [];
  deviceIds?.forEach(d=>ampereLabelsAC.push("OutputAmpereAC"+"-d-"+d))
  outputACIds?.forEach(d=>ampereLabelsAC.push("Ampere"+"-c-"+d))

  const frequencyLabels = showCombined ? ["OutputFrequency"] : [];
  deviceIds?.forEach(d=>frequencyLabels.push("OutputFrequency"+"-d-"+d))
  outputACIds?.forEach(d=>frequencyLabels.push("Frequency"+"-c-"+d))


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
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Voltage DC"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsDC} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Power DC in Ampere"}  min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsDC} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Voltage AC"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsAC} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Power AC in Ampere"}  min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsAC} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Output Frequency"}
                     timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
