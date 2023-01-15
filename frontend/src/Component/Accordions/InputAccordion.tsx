import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";

interface GridInputAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  maxSolarVoltage?: number
  deviceIds:Set<string>
  inputDCIds:Set<string>
  inputACIds:Set<string>
  showCombined: boolean
  getDeviceColour: (name:string)=>string
  timezone: string,
  hasAC: boolean
}

export default function InputAccordion({timezone,timeRange,graphData,maxSolarVoltage,getDeviceColour,showCombined,inputDCIds,inputACIds,deviceIds,hasAC}: GridInputAccordionProps) {

  let colors = [];
  let wattLabels:string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabels.push("InputWatt")
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d));
    wattLabels.push("InputWatt"+"-d-"+d)
  })
  inputDCIds?.forEach(d=>{
    colors.push(getDeviceColour("i-"+d));
    wattLabels.push("Watt"+"-i-"+d)
  })
  inputACIds?.forEach(d=>{
    colors.push(getDeviceColour("j-"+d));
    wattLabels.push("Watt"+"-j-"+d)
  })

  const voltLabelsDC = showCombined ? ["InputVoltageDC"] : [];
  deviceIds?.forEach(d=>voltLabelsDC.push("InputVoltageDC"+"-d-"+d))
  inputDCIds?.forEach(d=>voltLabelsDC.push("Voltage"+"-i-"+d))

  const ampereLabelsDC = showCombined ? ["InputAmpereDC"] : [];
  deviceIds?.forEach(d=>ampereLabelsDC.push("InputAmpereDC"+"-d-"+d))
  inputDCIds?.forEach(d=>ampereLabelsDC.push("Ampere"+"-i-"+d))

  const voltLabelsAC = showCombined ? ["InputVoltageAC"] : [];
  deviceIds?.forEach(d=>voltLabelsAC.push("InputVoltageAC"+"-d-"+d))
  inputACIds?.forEach(d=>voltLabelsAC.push("Voltage"+"-j-"+d))

  const ampereLabelsAC= showCombined ? ["InputAmpereAC"] : [];
  deviceIds?.forEach(d=>ampereLabelsAC.push("InputAmpereAC"+"-d-"+d))
  inputACIds?.forEach(d=>ampereLabelsAC.push("Ampere"+"-j-"+d))

  const frequencyLabels = showCombined ? ["InputFrequency"] : [];
  deviceIds?.forEach(d=>frequencyLabels.push("InputFrequency"+"-d-"+d))
  inputACIds?.forEach(d=>frequencyLabels.push("Frequency"+"-j-"+d))

  return<div>{graphData&&
 <Accordion defaultExpanded={true} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
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
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Power in Watt"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Voltage" + (hasAC ? "DC":"")} min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsDC} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Power in Ampere" + (hasAC ? "DC":"")} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsDC} />
        </div>
        {hasAC && <>
          <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Voltage AC"} min={0} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsAC} />
          </div>
          <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Power in Ampere AC"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsAC} />
          </div>
          <div className="defaultPanelWrapper">
            <LineGraph timezone={timezone} deviceColours={colors} legendOverrideValue={"Input Frequency"}
                       timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
          </div>
        </>}
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
