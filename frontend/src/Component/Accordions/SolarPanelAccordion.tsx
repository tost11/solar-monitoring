import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import Test from "../Test";
import {now} from "moment";
import Graph from "../Test";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  dashboardPath: String;
  refresh: string;
  timeRange: string;
}


export default function SolarPanelAccordion({timeRange,refresh,systemInfo, dashboardPath}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)

  const isLoading=()=>{
    return panel1Loading || panel2Loading
  }

  const changePanelStatus=()=>{
    if (isOpen) {
      setPanel1Loading(true)
      setPanel2Loading(true)
      setPanel3Loading(true)
    }
    setIsOpen(!isOpen)
  }

  useEffect(()=>{
    setPanel1Loading(true)
    setPanel2Loading(true)
    setPanel3Loading(true)
  },[timeRange])

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Solar</Typography>
    </AccordionSummary>
    <AccordionDetails>

        {isOpen && <div>
          {isLoading() && <CircularProgress/>}
          <div style={isLoading()?{display:'none'}:{}}>
            <div className="panelContainer">
              <div className="defaultPanelWrapper">
                <Graph timeRange={timeRange} systemInfo={systemInfo} onLoad={(r)=>setPanel1Loading(false)}/>
                <Graph timeRange={timeRange} systemInfo={systemInfo} onLoad={(r)=>setPanel2Loading(false)}/>
              </div>
            </div>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
