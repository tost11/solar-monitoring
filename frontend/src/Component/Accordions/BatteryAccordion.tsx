import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  dashboardPath: String;
  refresh: string;
}

export default function BatteryAccordion({refresh,systemInfo,dashboardPath}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)

  const isLoading=()=>{
    return panel1Loading || panel2Loading || panel3Loading
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
  },[refresh])

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>Battery</Typography>
    </AccordionSummary>
    <AccordionDetails>
      {isOpen && <div>
        {isLoading() && <CircularProgress/>}
        <div style={isLoading()?{display:'none'}:{}}>
          <div className="panelContainer">
            <div className="defaultPanelWrapper">
              <iframe
                src={dashboardPath+"?orgId=1&refresh="+refresh+"&theme=light&panelId=7"}
                onLoad={()=>setPanel1Loading(false)} width="450" height="200" frameBorder="0"/>
            </div>
            <div className="defaultPanelWrapper">
              <iframe
                src={dashboardPath+"?orgId=1&refresh="+refresh+"&theme=light&panelId=8"}
                onLoad={()=>setPanel2Loading(false)} width="450" height="200" frameBorder="0"/>
            </div>
            <div className="defaultPanelWrapper">
              <iframe
                src={dashboardPath+"?orgId=1&refresh="+refresh+"&theme=light&panelId=6"}
                onLoad={()=>setPanel3Loading(false)} width="450" height="200" frameBorder="0"/>
            </div>
          </div>
        </div>
      </div>}
    </AccordionDetails>
  </Accordion>
}
