import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import {GraphDataObject} from "../DetailDashboard";
import LineGraph from "../LineGraph";

interface AccordionProps {
  timeRange: string;
  graphData?:GraphDataObject
  labels:[string[]]
}
export default function ConsumptionAccordion({timeRange,graphData,labels}: AccordionProps) {


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
            {labels.map((value,index)=>{
              return <LineGraph key={index} timeRange={timeRange} graphData={graphData} labels={value}/>
            })}
          </div>
        </div>
      </AccordionDetails>
    </Accordion>}
    </div>
  }

