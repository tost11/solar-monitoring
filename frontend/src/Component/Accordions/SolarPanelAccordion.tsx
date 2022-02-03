import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import RefreshTimeSelector from "../RefreshTimeSelector";
import React, {useEffect, useState} from "react";
import {Simulate} from "react-dom/test-utils";
import input = Simulate.input;

interface AccordionProps {
  name: string
  grafanaUid: string
}

export default function SolarPanelAccordion({name, grafanaUid}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)
  const [refreshTime,setRefreshTime] = useState("10s")

  const isLoading=()=>{
    return panel1Loading || panel2Loading || panel3Loading
  }
  useEffect(() => {
    setPanel1Loading(false)
    setPanel2Loading(false)
    setPanel3Loading(false)
  },[refreshTime])
  return <Accordion className={"DetailAccordion"} onChange={() => {
    console.log(isOpen)
    if (isOpen) {
      setPanel1Loading(true)
      setPanel2Loading(true)
      setPanel3Loading(true)
    }
    setIsOpen(!isOpen)
  }}>
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
            <RefreshTimeSelector setRefreshTime={(r)=>{setRefreshTime(r)}} refreshTime={refreshTime}/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=0"}
              onLoad={()=>setPanel1Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=1"}
              onLoad={()=>setPanel2Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh="+refreshTime+"&theme=light&panelId=4"}
              onLoad={()=>setPanel3Loading(false)} width="450" height="200" frameBorder="0"/>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
