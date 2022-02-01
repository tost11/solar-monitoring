import React, {useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDTO} from "../../api/SolarSystemAPI";

interface AccordionProps {
  systemInfo: SolarSystemDTO
}

export default function ConsumptionAccordion({systemInfo}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [isOpen, setIsOpen] = useState(false)

  const isLoading=()=>{
    return panel1Loading || panel2Loading || panel3Loading
  }

  const changePannelStatus=()=>{
    if (isOpen) {
      setPanel1Loading(true)
      setPanel2Loading(true)
      setPanel3Loading(true)
    }
    setIsOpen(!isOpen)
  }

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePannelStatus}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
    >
      <Typography>Consumption</Typography>
    </AccordionSummary>
    <AccordionDetails>
      <div>
        {isOpen && <div>
          {isLoading() && <CircularProgress/>}
          <div style={isLoading()?{display:'none'}:{}}>
            <div className="defaultFlowColumn">
              <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
                <div style={{margin: "4px"}}>
                  Usage over the last * Hours
                </div>
                <iframe
                    src={"/grafana/d-solo/dashboard-" + systemInfo.id+"/dashboard-" + systemInfo.id+"?orgId=1&refresh=30s&theme=light&panelId=7"}
                    onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
              </div>
              <div className="panelContainer">
                <div className="defaultPanelWrapper">
                  <div style={{margin: "4px"}}>
                    Current Battery Values over the last * Hours
                  </div>
                  <iframe
                    src={"/grafana/d-solo/dashboard-" + systemInfo.id+"/dashboard-" + systemInfo.id+ "?orgId=1&refresh=30s&theme=light&panelId=5"}
                    onLoad={()=>setPanel1Loading(false)} width="450px" height="200px" frameBorder="0"/>
                </div>
                <div className="defaultPanelWrapper">
                  <div style={{margin: "4px"}}>
                    Current Voltage over the last * Hours
                  </div>
                  <iframe
                      src={"/grafana/d-solo/dashboard-" + systemInfo.id+"/dashboard-" + systemInfo.id+ "?orgId=1&refresh=30s&theme=light&panelId=6"}
                      onLoad={()=>setPanel2Loading(false)} width="450px" height="200px" frameBorder="0"/>
                </div>
              </div>
            </div>
          </div>
        </div>}
      </div>
    </AccordionDetails>
  </Accordion>
}
