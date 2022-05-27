import React from "react";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {GraphDataObject} from "../../views/SystemDashboardView";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData?:GraphDataObject
  inverter: boolean
  device: boolean
  inverterVoltage?: number
  timezone?  :string
}
export default function ConsumptionAccordion({timezone,timeRange,graphData,inverter,device,inverterVoltage}: AccordionProps) {

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
            <LineGraph  min={0} unit="W"  timezone={timezone} timeRange={timeRange} graphData={graphData} labels={consLabels} />
          </div>
          {device &&
              <div className="defaultPanelWrapper">
                <LineGraph min={0} unit="W" timezone={timezone} timeRange={timeRange} graphData={graphData} labels={["ConsumptionDeviceWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph min={0} unit="W" timezone={timezone} timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterWatt"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph timezone={timezone} min={inverterVoltage?inverterVoltage-5:undefined} max={inverterVoltage?inverterVoltage+5:undefined} unit="V" timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterVoltage"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph unit="HZ" timezone={timezone}  timeRange={timeRange} graphData={graphData} labels={["inverterFrequency"]}/>
              </div>
          }
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

