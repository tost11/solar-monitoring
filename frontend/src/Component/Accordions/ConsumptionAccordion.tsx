import React from "react";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";

interface AccordionProps {
  timeRange: string;
  graphData?:GraphDataObject
  inverter: boolean;
  device: boolean;
}
export default function ConsumptionAccordion({timeRange,graphData,inverter,device}: AccordionProps) {

    let consLabels = ["TotalConsumption"]
    if(inverter && device){
      consLabels.push("ConsumptionInverterWatt")
      //TODO add when datastructure is fixed
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
            <LineGraph timeRange={timeRange} graphData={graphData} labels={consLabels} />
          </div>
          {/*TODO add when datastructure is fixed*/}
          {device &&
              <div className="defaultPanelWrapper">
                <LineGraph timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterVoltage"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterVoltage"]}/>
              </div>
          }
          {inverter &&
              <div className="defaultPanelWrapper">
                <LineGraph timeRange={timeRange} graphData={graphData} labels={["ConsumptionInverterAmpere"]}/>
              </div>
          }
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

