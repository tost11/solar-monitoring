import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  dashboardPath: String;
  refresh: string;
  timeRange: string;
}


export default function ConsumptionAccordion({timeRange,refresh,systemInfo,dashboardPath}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)

  const [isOpen, setIsOpen] = useState(false)

  const isLoading=()=>{
    return panel1Loading
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
      <Typography>Consumption</Typography>
    </AccordionSummary>



    <AccordionDetails>
        {isOpen && <div>
          {isLoading() && <CircularProgress/>}
          <div style={isLoading()?{display:'none'}:{}}>
            <div className="defaultFlowColumn">
              <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
                <div style={{margin: "4px"}}>
                  Usage over the last * Hours
                </div>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh="+refresh+"&from=now-"+timeRange+"&theme=light&panelId=12"}
                  onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                {systemInfo.type=="SELFMADE_CONSUMPTION" && <div>
                  <iframe
                  src={dashboardPath+"?orgId=1&refresh="+refresh+"&from=now-"+timeRange+"&theme=light&panelId=2"}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                  </div>
                }
              </div>

              {
                /*<div className="panelContainer">
                <div className="defaultPanelWrapper">
                  <div style={{margin: "4px"}}>
                    Current Battery Values over the last * Hours
                  </div>
                  <iframe
                    src={dashboardPath+"?orgId=1&refresh="+refresh+"&theme=light&panelId=5"}
                    onLoad={()=>setPanel1Loading(false)} width="450px" height="200px" frameBorder="0"/>
                </div>
                <div className="defaultPanelWrapper">
                  <div style={{margin: "4px"}}>
                    Current Voltage over the last * Hours
                  </div>
                  <iframe
                      src={dashboardPath+"?orgId=1&refresh="+refresh+"&theme=light&panelId=5"}
                      onLoad={()=>setPanel2Loading(false)} width="450px" height="200px" frameBorder="0"/>
                </div>
              </div>*/}
            </div>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
