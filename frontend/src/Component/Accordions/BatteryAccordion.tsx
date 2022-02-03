import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useEffect, useState} from "react";
import RefreshTimeSelector from "../RefreshTimeSelector";

interface AccordionProps {
  name: string
  grafanaUid: string
}

export default function BatteryAccordion({name, grafanaUid}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)
  const [refreshTime,setRefreshTime] = useState("10s")

  const isLoading=()=>{
    return panel1Loading || panel2Loading || panel3Loading
  }
  useEffect(() => {
    setPanel1Loading(true)
    setPanel2Loading(true)
    setPanel3Loading(true)
  },[refreshTime])

  return <Accordion className={"DetailAccordion"} onChange={() => {
    console.log(isOpen)
    if (isOpen) {
      setPanel1Loading(true)
      setPanel2Loading(true)
      setPanel3Loading(true)
    }
    setIsOpen(!isOpen)
  }
  }>
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
            <RefreshTimeSelector setRefreshTime={(r)=>{setRefreshTime(r)}} refreshTime={refreshTime}/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=3"}
              onLoad={()=>setPanel1Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=6"}
              onLoad={()=>setPanel2Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=2"}
              onLoad={()=>setPanel3Loading(false)} width="450" height="200" frameBorder="0"/>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
