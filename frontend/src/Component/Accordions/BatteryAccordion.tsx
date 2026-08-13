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

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData: GraphDataObject
  batteryVoltage?:number
  deviceIds:Set<string>
  batteryIds:Set<string>
  minBatteryVoltage?: number
  maxBatteryVoltage?: number
  timezone?  :string,
  showCombined :boolean,
  getDeviceColour: (name:string)=>string,
  namings: {[key: string]: string}
  defaultDuration?: number
  graphFilter?: string[]
}

export default function BatteryAccordion({graphFilter,defaultDuration,namings,timezone,timeRange,graphData,minBatteryVoltage,maxBatteryVoltage,deviceIds,batteryIds,showCombined,getDeviceColour}: AccordionProps) {

  const { t } = useTranslation()

  const isFiltered = (filter: string) => graphFilter?.includes(filter) || false;

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
  <Accordion defaultExpanded style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      id="panel1a-header"
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("common.battery")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        {!isFiltered("BATTERY_WATT") && <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.battery_label_watt")} timeRange={timeRange} unit="W" graphData={graphData} labels={wattLabels} />
        </div>}
        {!isFiltered("BATTERY_VOLTAGE") && <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.battery_label_voltage")} min={minBatteryVoltage} max={maxBatteryVoltage} timeRange={timeRange} unit="V" graphData={graphData} labels={voltLabels} />
        </div>}
        {!isFiltered("BATTERY_AMPERE") && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.battery_label_ampere")}
                       timeRange={timeRange} unit="A" graphData={graphData} labels={ampereLabels}/>
          </div>
        }
        {!isFiltered("BATTERY_SOC") && <div className="defaultPanelWrapper">
          <LineGraph defaultDuration={defaultDuration} valueNameOverrides={namings}  timezone={timezone} deviceColours={colors} min={0} timeRange={timeRange} unit="%" graphData={graphData} labels={["BatteryPercentage"]} legendOverrideValue={t("components.graph_accordion.battery_label_soc")} />
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>}
  </div>
}
