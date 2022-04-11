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
  deviceIds?:number[]
}

export default function GridOutputAccordion({timeRange,graphData,gridVoltage,deviceIds}: GridOutputAccordionProps) {

  const wattLabels = ["GridWatt"];
  deviceIds?.forEach(d=>wattLabels.push("GridWatt_"+d))

  const voltLabels = ["GridVoltage"];
  deviceIds?.forEach(d=>voltLabels.push("GridVoltage_"+d))

  const ampereLabels = ["GridAmpere"];
  deviceIds?.forEach(d=>ampereLabels.push("GridAmpere_"+d))

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
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={gridVoltage?gridVoltage-5:undefined} max={gridVoltage?gridVoltage+5:undefined} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
