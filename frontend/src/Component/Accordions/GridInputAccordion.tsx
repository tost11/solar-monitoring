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
  deviceIds?:number[]
}

export default function GridInputAccordion({timeRange,graphData,maxSolarVoltage,deviceIds}: GridInputAccordionProps) {

  const wattLabels = ["ChargeWatt"];
  deviceIds?.forEach(d=>wattLabels.push("ChargeWatt_"+d))

  const voltLabels = ["ChargeVoltage"];
  deviceIds?.forEach(d=>voltLabels.push("ChargeVoltage_"+d))

  const ampereLabels = ["ChargeAmpere"];
  deviceIds?.forEach(d=>ampereLabels.push("ChargeAmpere_"+d))

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
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabels} />
        </div>
        <div className="defaultPanelWrapper">
            <LineGraph min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
