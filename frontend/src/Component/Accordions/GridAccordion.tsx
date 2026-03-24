import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";
import {useTranslation} from "react-i18next";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData: GraphDataObject
  deviceIds: Set<string>
  gridIds: Set<string>
  timezone?: string
  showCombined: boolean
  getDeviceColour: (name: string) => string
  showAmpere: boolean
  namings: {[key: string]: string}
  defaultDuration?: number
}

export default function GridAccordion({defaultDuration, namings, timezone, timeRange, graphData, deviceIds, gridIds, showCombined, getDeviceColour, showAmpere}: AccordionProps) {

  const { t } = useTranslation()

  let colors = [];
  let wattLabels: string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabels.push("GridWatt")
  }

  deviceIds?.forEach(d => {
    colors.push(getDeviceColour("d-" + d));
    wattLabels.push("GridWatt" + "-d-" + d)
  })
  gridIds?.forEach(d => {
    colors.push(getDeviceColour("g-" + d));
    wattLabels.push("Watt" + "-g-" + d)
  })

  const voltLabels = showCombined ? ["GridVoltage"] : [];
  deviceIds?.forEach(d => voltLabels.push("GridVoltage" + "-d-" + d))
  gridIds?.forEach(d => voltLabels.push("Voltage" + "-g-" + d))

  const ampereLabels = showCombined ? ["GridAmpere"] : [];
  deviceIds?.forEach(d => ampereLabels.push("GridAmpere" + "-d-" + d))
  gridIds?.forEach(d => ampereLabels.push("Ampere" + "-g-" + d))

  const frequencyLabels = showCombined ? ["GridFrequency"] : [];
  deviceIds?.forEach(d => frequencyLabels.push("GridFrequency" + "-d-" + d))
  gridIds?.forEach(d => frequencyLabels.push("Frequency" + "-g-" + d))

  return <div>{graphData &&
  <Accordion defaultExpanded={true} style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      id="panel1a-header"
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("common.grid")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.grid_label_watt")} timeRange={timeRange} unit="W" graphData={graphData} labels={wattLabels} />
        </div>
        <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.grid_label_voltage")} timeRange={timeRange} unit="V" graphData={graphData} labels={voltLabels} />
        </div>
        {showAmpere && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.grid_label_ampere")}
                       timeRange={timeRange} unit="A" graphData={graphData} labels={ampereLabels}/>
          </div>
        }
        <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.grid_label_frequency")} timeRange={timeRange} unit="Hz" graphData={graphData} labels={frequencyLabels} />
        </div>
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
