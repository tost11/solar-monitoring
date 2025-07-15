import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";
import {NamingsDTO} from "../../api/SolarSystemAPI";
import {useTranslation} from "react-i18next";

interface MoreAccordionProps {
  timeRange: TimeAndDuration
  graphData:GraphDataObject
  deviceIds:Set<string>
  showCombined: boolean
  getDeviceColour: (name:string)=>string
  timezone: string,
  namings: {[key: string]: string}
  defaultDuration?: number
}

export default function MoreAccordion({defaultDuration,namings,timezone,timeRange,graphData,getDeviceColour,showCombined,deviceIds}: MoreAccordionProps) {

  const { t } = useTranslation()

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
 <Accordion defaultExpanded={true} style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("common.temperature")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors}
                       legendOverrideValue={t("common.temperature")} min={0} timeRange={timeRange} graphData={graphData}
                       unit="°C" labels={temperatureLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
