import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData: GraphDataObject
  batteryVoltage?:number
  deviceIds:Set<string>
  batteryIds:Set<string>
  isBatteryPercentage?: boolean
  minBatteryVoltage?: number
  maxBatteryVoltage?: number
  timezone?  :string,
  showCombined :boolean,
  getDeviceColour: (name:string)=>string,
  showAmpere: boolean,
  namings: {[key: string]: string}
  defaultDuration?: number
}

export default function BatteryAccordion({defaultDuration,namings,timezone,timeRange,graphData,isBatteryPercentage,minBatteryVoltage,maxBatteryVoltage,deviceIds,batteryIds,showCombined,getDeviceColour,showAmpere}: AccordionProps) {

  let colors = [];
  let wattLabels:string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabels.push("BatteryWatt")
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d));
    wattLabels.push("BatteryWatt"+"-d-"+d)
  })
  batteryIds?.forEach(d=>{
    colors.push(getDeviceColour("b-"+d));
    wattLabels.push("Watt"+"-b-"+d)
  })

  const voltLabels = showCombined ? ["BatteryVoltage"] : [];
  deviceIds?.forEach(d=>voltLabels.push("BatteryVoltage"+"-d-"+d))
  batteryIds?.forEach(d=>voltLabels.push("Voltage"+"-b-"+d))

  const ampereLabels = showCombined ? ["BatteryAmpere"] : [];
  deviceIds?.forEach(d=>ampereLabels.push("BatteryAmpere"+"-d-"+d))
  batteryIds?.forEach(d=>ampereLabels.push("Ampere"+"-b-"+d))

  return <div>{graphData &&
  <Accordion defaultExpanded={true} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography><b>Battery</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} legendOverrideValue={"Battery usage in Watt"} timeRange={timeRange} unit="W" graphData={graphData} labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={"Battery Voltage"} min={minBatteryVoltage} max={maxBatteryVoltage} timeRange={timeRange} unit="V" graphData={graphData} labels={voltLabels} />
        </div>
        {showAmpere && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} legendOverrideValue={"Battery usage in Ampere"}
                       timeRange={timeRange} unit="A" graphData={graphData} labels={ampereLabels}/>
          </div>
        }
        {isBatteryPercentage && <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} min={0} timeRange={timeRange} unit="%" graphData={graphData} labels={["BatteryPercentage"]} />
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
