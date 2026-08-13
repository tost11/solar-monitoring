import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Typography from "@mui/material/Typography";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {getGraphColourByIndex} from "../utils/GraphUtils";
import {GraphDataObject} from "../../api/GraphAPI";
import {useTranslation} from "react-i18next";

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
  namings: {[key: string]: string}
  defaultDuration?: number
  graphFilter?: string[]
}

export default function InputAccordion({graphFilter,defaultDuration,namings,timezone,timeRange,graphData,maxSolarVoltage,getDeviceColour,showCombined,inputDCIds,inputACIds,deviceIds}: GridInputAccordionProps) {

  const { t } = useTranslation()

  const isFiltered = (filter: string) => graphFilter?.includes(filter) || false;

  // Determine if both AC and DC are visible
  const bothACAndDCVisible = !isFiltered("INPUT_WATT_AC") && !isFiltered("INPUT_WATT_DC")

  // Combined graph labels - always includes InputWatt, conditionally includes AC/DC
  let wattLabelsCombined: string[] = bothACAndDCVisible ? ["InputWatt", "InputWattAC", "InputWattDC"] : ["InputWatt"]

  let colors = [];
  let wattLabelsAC:string[] = []
  let wattLabelsDC:string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabelsDC.push("InputWattDC")
    wattLabelsAC.push("InputWattAC")
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d))
    wattLabelsDC.push("InputWattDC"+"-d-"+d)
    wattLabelsAC.push("InputWattAC"+"-d-"+d)
  })
  inputDCIds?.forEach(d=>{
    colors.push(getDeviceColour("i-"+d))
    wattLabelsDC.push("Watt"+"-i-"+d)
  })
  inputACIds?.forEach(d=>{
    colors.push(getDeviceColour("j-"+d))
    wattLabelsAC.push("Watt"+"-j-"+d)
  })

  // Colors for combined graph
  let combinedColors = [getGraphColourByIndex(0)]  // InputWatt color
  if (bothACAndDCVisible) {
    combinedColors.push(getGraphColourByIndex(1))  // InputWattAC color
    combinedColors.push(getGraphColourByIndex(2))  // InputWattDC color
  }

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
 <Accordion defaultExpanded style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("system_common.input")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        {!isFiltered("INPUT_WATT_COMBINED") && <div className="fullWidthPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={combinedColors} legendOverrideValue={t("components.graph_accordion.input_label_watt")} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsCombined} />
        </div>}
        {!isFiltered("INPUT_WATT_DC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_watt")+" DC"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsDC} />
        </div>}
        {!isFiltered("INPUT_VOLTAGE_DC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_voltage")} min={0} max={maxSolarVoltage} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsDC} />
        </div>}
        {!isFiltered("INPUT_AMPERE_DC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_ampere")} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsDC}/>
          </div>
        }
        {!isFiltered("INPUT_WATT_AC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_watt")+" AC"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsAC} />
          </div>}
        {!isFiltered("INPUT_VOLTAGE_AC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_voltage")+" AC"} min={0} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsAC} />
          </div>}
        {!isFiltered("INPUT_AMPERE_AC") && <div className="defaultPanelWrapper">
                <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_ampere")+" AC"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsAC}/>
              </div>
          }
        {!isFiltered("INPUT_FREQUENCY") && <div className="defaultPanelWrapper">
              <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.input_label_frequency")} timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
          </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
