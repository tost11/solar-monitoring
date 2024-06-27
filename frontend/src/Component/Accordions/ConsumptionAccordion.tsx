import React from "react";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import LineGraph from "../LineGraph";
import {TimeAndDuration} from "../time/TimeAndDateSelector";
import {GraphDataObject} from "../../api/GraphAPI";

interface AccordionProps {
  timeRange: TimeAndDuration
  graphData?:GraphDataObject
  inverter: boolean
  device: boolean
  inverterVoltage?: number
  timezone?  :string
  defaultDuration?: number
}
export default function ConsumptionAccordion({defaultDuration,timezone,timeRange,graphData,inverter,device,inverterVoltage}: AccordionProps) {

    let consLabels = ["OutputWatt"]
    //TODO by device
    /*if(inverter && device){
      consLabels.push("ConsumptionInverterWatt")
      consLabels.push("ConsumptionDeviceWatt")
    }*/

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
            <LineGraph defaultDuration={defaultDuration}  min={0} unit="W"  timezone={timezone} timeRange={timeRange} graphData={graphData} labels={consLabels} />
          </div>
          <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  min={0} unit="A"  timezone={timezone} timeRange={timeRange} graphData={graphData} labels={["OutputAmpere"]} />
          </div>
          <div className="defaultPanelWrapper">
            <LineGraph defaultDuration={defaultDuration}  min={0} unit="V"  timezone={timezone} timeRange={timeRange} graphData={graphData} labels={["OutputVoltage"]} />
          </div>
          {/*//TODO refactor
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
          }*/}
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

