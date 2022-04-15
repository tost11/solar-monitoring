import React from "react";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../../context/time/TimeAndDateSelector";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData?:GraphDataObject
  inverter: boolean
  device: boolean
  inverterVoltage?: number
}
export default function ConsumptionAccordion({timeRange,graphData,inverter,device,inverterVoltage}: AccordionProps) {

    let consLabels = ["TotalConsumption"]
    if(inverter && device){
      consLabels.push("ConsumptionInverterWatt")
      consLabels.push("ConsumptionDeviceWatt")
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
            <LineGraph  min={0} unit="W" timeRange={timeRange} graphData={graphData} labels={consLabels} />
          </div>
          {device &&
              <div className="defaultPanelWrapper">
                <LineGraph min={0} unit="W" timeRange={timeRange} graphData={graphData} labels={["ConsumptionDeviceWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph min={0} unit="W" timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph min={inverterVoltage?inverterVoltage-5:undefined} max={inverterVoltage?inverterVoltage+5:undefined} unit="V" timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterVoltage"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph unit="HZ" timeRange={timeRange} graphData={graphData} labels={["inverterFrequency"]}/>
              </div>
          }
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

