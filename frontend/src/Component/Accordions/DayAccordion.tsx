import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";

interface AccordionProps {
  systemInfo: SolarSystemDTO;
  dashboardPath: String;
}

export default function DayAccordion({systemInfo,dashboardPath}:AccordionProps) {
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

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
  <AccordionSummary
    expandIcon={<ExpandMoreIcon/>}
    aria-controls="panel1a-content"
    id="panel1a-header"
  >
    <Typography>Day</Typography>
  </AccordionSummary>
  <AccordionDetails>
    {isOpen && <div>
      {isLoading() && <CircularProgress/>}
      <div style={isLoading()?{display:'none'}:{}}>
        <iframe
          src={dashboardPath+"?orgId=1&refresh=1d&theme=light&panelId=3"} onLoad={()=>setPanel1Loading(false)} width="450" height="200" frameBorder="0"/>
        <iframe
          src={dashboardPath+"?orgId=1&refresh=1d&theme=light&panelId=6"} onLoad={()=>setPanel2Loading(false)} width="450" height="200" frameBorder="0"/>
        <iframe
          src={dashboardPath+"?orgId=1&refresh=1d&theme=light&panelId=2"} onLoad={()=>setPanel3Loading(false)} width="450" height="200" frameBorder="0"/>
      </div>
    </div>}
  </AccordionDetails>
</Accordion>
}
