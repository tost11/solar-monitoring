import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";
import {NamingsDTO} from "../../api/SolarSystemAPI";

interface MoreAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  deviceIds:Set<string>
  showCombined: boolean
  getDeviceColour: (name:string)=>string
  timezone: string,
  namings: {[key: string]: string}
}

export default function MoreAccordion({namings,timezone,timeRange,graphData,getDeviceColour,showCombined,deviceIds}: MoreAccordionProps) {

  let colors = [];

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d))
  })

  const temperatureLabels = showCombined ? ["Temperature"] : [];
  deviceIds?.forEach(d=>temperatureLabels.push("Temperature"+"-d-"+d))

  return<div>{graphData&&
 <Accordion defaultExpanded={true} style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography><b>Temperature</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">

        <div className="defaultPanelWrapper">
            <LineGraph valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={"Temperature"} min={0} timeRange={timeRange} graphData={graphData} unit="C" labels={temperatureLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
