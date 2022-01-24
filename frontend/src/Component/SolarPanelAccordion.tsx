import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";

interface AccordionProps{
  name:string
  grafanaUid:string
}
export default function SolarPanelAccordion({name,grafanaUid}:AccordionProps) {
  const [isFrameLoading, setIsFrameLoading] = useState(true)
  const [isOpen,setIsOpen] =useState(false)
  let counter= 0;
  const hiddenSpinner=()=>{
    counter = counter +1;
    if(counter==3) {
      setIsFrameLoading(false)
    }
  }
return<Accordion className={"DetailAccordion"} onChange={()=>{

    if(isOpen) {
      setIsFrameLoading(true)
    }
    setIsOpen(!isOpen)}
}>
  <AccordionSummary
    expandIcon={<ExpandMoreIcon/>}
    aria-controls="panel1a-content"
    id="panel1a-header"
  >
    <Typography>Solar Panel</Typography>
  </AccordionSummary>
  <AccordionDetails>
    {isOpen && <div>
      {isFrameLoading && <CircularProgress/>}
      <div hidden={isFrameLoading}>
        <iframe
          src={"/grafana/d-solo/" + grafanaUid + "/generated-" + name + "?orgId=1&theme=light&panelId=3"}
          onLoad={hiddenSpinner} width="450" height="200" frameBorder="0"/>
        <iframe
          src={"/grafana/d-solo/"+grafanaUid+"/generated-"+name+"?orgId=1&theme=light&panelId=6"} onLoad={hiddenSpinner} width="450" height="200" frameBorder="0"/>
        <iframe
          src={"/grafana/d-solo/"+grafanaUid+"/generated-"+name+"?orgId=1&theme=light&panelId=2"} onLoad={hiddenSpinner} width="450" height="200" frameBorder="0"/>
      </div>
    </div>}
  </AccordionDetails>
</Accordion>
}
