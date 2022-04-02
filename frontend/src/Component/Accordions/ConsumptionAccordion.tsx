import React from "react";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";

interface AccordionProps {
  timeRange: string
  graphData?:GraphDataObject
  inverter: boolean
  device: boolean
  inverterVoltage?: number
}
export default function ConsumptionAccordion({timeRange,graphData,inverter,device,inverterVoltage}: AccordionProps) {

    let consLabels = ["TotalConsumption"]
    if(inverter && device){
      consLabels.push("ConsumptionInverterWatt")
      consLabels.push("consumptionDeviceWatt")
    }

    return <div>{graphData&&
    <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
      >
        <Typography>Consumption</Typography>
      </AccordionSummary>
      <AccordionDetails>
        <div className="panelContainer">
          <div className="defaultPanelWrapper">
            <LineGraph  unit="W" timeRange={timeRange} graphData={graphData} labels={consLabels} />
          </div>
          {device &&
              <div className="defaultPanelWrapper">
                <LineGraph unit="W" timeRange={timeRange} graphData={graphData} labels={["consumptionDeviceWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph unit="W" timeRange={timeRange} graphData={graphData} labels={["consumptionInverterWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph min={inverterVoltage?inverterVoltage-5:undefined} max={inverterVoltage?inverterVoltage+5:undefined} unit="V" timeRange={timeRange} graphData={graphData} labels={["consumptionInverterVoltage"]}/>
              </div>
          }
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

