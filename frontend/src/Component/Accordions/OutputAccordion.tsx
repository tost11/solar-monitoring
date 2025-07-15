import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
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
  hasAC: boolean,
  hasDC: boolean,
  showAmpere: boolean,
  namings: {[key: string]: string}
  defaultDuration?: number
}

export default function OutputAccordion({defaultDuration,namings,timezone,timeRange,graphData,outputDCIds,outputACIds,deviceIds,showCombined,getDeviceColour,hasAC,hasDC,showAmpere}: GridOutputAccordionProps) {

  const { t } = useTranslation()

  let colors = [];
  let wattLabels:string[] = ["OutputWatt","OutputWattDC","OutputWattAC"]
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

  const differentOutputs = hasDC && hasAC

return<div>{graphData&&
 <Accordion defaultExpanded={true} style={{backgroundColor:"snow"}} className={"DetailAccordion"}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      style={{backgroundColor:"lightblue"}}
    >
      <Typography><b>{t("system_common.output")}</b></Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div className="panelContainer">
        {hasAC && hasDC && <div style={{width:"100%",marginBottom: "15px"}}>
          <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={[getGraphColourByIndex(0),"green","red"]} min={0}
                                                                               timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabels}/></div>
        }
        {hasDC && <>
          <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_watt")+(differentOutputs?" DC":"")+" in Watt" + (differentOutputs?" DC":"")} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsDC} />
          </div>
          <div className="defaultPanelWrapper">
              <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_voltage")+(differentOutputs?" DC":"")} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsDC} />
          </div>
          {showAmpere && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_ampere") + (differentOutputs ? " DC" : "")} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsDC}/>
          </div>
          }
        </>}
        {hasAC && <>
          <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_watt")+(differentOutputs?" AC":"")} min={0} timeRange={timeRange} graphData={graphData} unit="W" labels={wattLabelsAC} />
          </div>
          <div className="defaultPanelWrapper">
              <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_voltage")+(differentOutputs?" AC":"")} timeRange={timeRange} graphData={graphData} unit="V" labels={voltLabelsAC} />
          </div>
          {showAmpere && <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_ampere") + (differentOutputs ? " AC" : "") + " in Ampere"} min={0} timeRange={timeRange} graphData={graphData} unit="A" labels={ampereLabelsAC}/>
            </div>
          }
          <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  valueNameOverrides={namings} timezone={timezone} deviceColours={colors} legendOverrideValue={t("components.graph_accordion.output_label_frequency")}
                       timeRange={timeRange} graphData={graphData} unit="HZ" labels={frequencyLabels}/>
          </div>
        </>}
      </div>
    </AccordionDetails>
  </Accordion>}
</div>
}
