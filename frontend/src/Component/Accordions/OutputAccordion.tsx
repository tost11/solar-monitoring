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

interface GridOutputAccordionProps {
  timeRange: TimeAndDuration
  graphData: GraphDataObject
  deviceIds: Set<string>
  outputDCIds: Set<string>
  outputACIds: Set<string>
  showCombined: boolean,
  timezone?  :string,
  getDeviceColour: (name:string)=>string,
  namings: {[key: string]: string}
  defaultDuration?: number
  graphFilter?: string[]
}

export default function OutputAccordion({graphFilter,defaultDuration,namings,timezone,timeRange,graphData,outputDCIds,outputACIds,deviceIds,showCombined,getDeviceColour}: GridOutputAccordionProps) {

  const { t } = useTranslation()

  const isFiltered = (filter: string) => graphFilter?.includes(filter) || false;

  // Determine if both AC and DC are visible
  const bothACAndDCVisible = !isFiltered("OUTPUT_WATT_AC") && !isFiltered("OUTPUT_WATT_DC")

  // Combined graph labels - always includes OutputWatt and TotalConsumptionWatt, conditionally includes AC/DC
  let wattLabelsCombined: string[] = bothACAndDCVisible
    ? ["OutputWatt", "OutputWattAC", "OutputWattDC", "TotalConsumptionWatt"]
    : ["OutputWatt", "TotalConsumptionWatt"]

  let colors = [];
  let wattLabelsAC:string[] = []
  let wattLabelsDC:string[] = []

  if(showCombined) {
    colors.push(getGraphColourByIndex(0))
    wattLabelsDC.push("OutputWattDC")
    wattLabelsAC.push("OutputWattAC")
  }

  deviceIds?.forEach(d=>{
    colors.push(getDeviceColour("d-"+d));
    wattLabelsDC.push("OutputWattDC"+"-d-"+d)
    wattLabelsAC.push("OutputWattAC"+"-d-"+d)
  })
  outputDCIds?.forEach(d=>{
    colors.push(getDeviceColour("o-"+d));
    wattLabelsDC.push("Watt"+"-o-"+d)
  })
  outputACIds?.forEach(d=>{
    colors.push(getDeviceColour("c-"+d));
    wattLabelsAC.push("Watt"+"-c-"+d)
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

  // Colors for combined graph
  let combinedColors = bothACAndDCVisible
    ? [getGraphColourByIndex(0), getGraphColourByIndex(1), getGraphColourByIndex(2), "black"]
    : [getGraphColourByIndex(0), "black"]

  // Define calculation function for total consumption watt
  const calculatedFieldsWatt = {
    "TotalConsumptionWatt": (point: any) => {
      if (point["GridWatt"] == null) {
        return null;
      }
      const outputValue = point["OutputWatt"] ?? 0;
      return Math.max(0, outputValue + point["GridWatt"]);
    }
  };

return<div>{graphData&&
 <Accordion defaultExpanded style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("system_common.output")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        {!isFiltered("OUTPUT_WATT_COMBINED") && <div className="fullWidthPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={combinedColors} legendOverrideValue={t("components.graph_accordion.output_label_watt")} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsCombined} calculatedFields={calculatedFieldsWatt} />
        </div>}
        {!isFiltered("OUTPUT_WATT_DC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_watt")+" DC"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsDC} />
          </div>}
        {!isFiltered("OUTPUT_VOLTAGE_DC") && <div className="defaultPanelWrapper">
              <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_voltage")+" DC"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsDC} />
          </div>}
        {!isFiltered("OUTPUT_AMPERE_DC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_ampere")+" DC"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsDC}/>
          </div>
          }
        {!isFiltered("OUTPUT_WATT_AC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_watt")+" AC"} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsAC} />
          </div>}
        {!isFiltered("OUTPUT_VOLTAGE_AC") && <div className="defaultPanelWrapper">
              <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_voltage")+" AC"} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsAC} />
          </div>}
        {!isFiltered("OUTPUT_AMPERE_AC") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_ampere")+" AC"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsAC}/>
            </div>
          }
        {!isFiltered("OUTPUT_FREQUENCY") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_frequency")}
                       timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
          </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
