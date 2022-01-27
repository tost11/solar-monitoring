import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";

interface AccordionProps {
  name: string
  grafanaUid: string
}

export default function SolarPanelAccordion({name, grafanaUid}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)

  const isLoading=()=>{
    console.log("--------------")
    console.log(panel1Loading)
    console.log(panel2Loading)
    console.log(panel3Loading)
    return panel1Loading && panel2Loading && panel3Loading;
  }

  return <Accordion className={"DetailAccordion"} onChange={() => {
    console.log("ji")
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
      <Typography>Solar</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div>
        {isOpen && <div>
          {isLoading() && <CircularProgress/>}
          <div hidden={isLoading()}>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh=1d&theme=light&panelId=0"}
              onLoad={()=>setPanel1Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh=1d&theme=light&panelId=1"}
              onLoad={()=>setPanel2Loading(false)} width="450" height="200" frameBorder="0"/>
            <iframe
              src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&refresh=1d&theme=light&panelId=4"}
              onLoad={()=>setPanel3Loading(false)} width="450" height="200" frameBorder="0"/>
          </div>
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>
}
