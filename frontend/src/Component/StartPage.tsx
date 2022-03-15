import React, {useEffect, useState} from "react";
import {fetchStartpageSystems} from "../api/SolarSystemAPI";
import {Accordion, AccordionDetails, AccordionSummary, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";

export default function StartPage(){

  const [systemIds,setSystemIds] = useState<{ids:Number[]}>()

  useEffect(()=>{
    fetchStartpageSystems().then(res=>{
      console.log(res);
      setSystemIds({ids:res})
    })
  },[])

  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>Hallo and Welcome to your Mage Page for your Solar systems</h1>

    {systemIds && <div>{systemIds.ids.map((s)=>{

      const dashboardPath = "/grafana/d-solo/dashboard-" + s+"/dashboard-" + s
      const refreshTime = "30s"
      const timeRange = "3h"

      return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"}>
        <AccordionSummary
            expandIcon={<ExpandMoreIcon/>}
            aria-controls="panel1a-content"
            id="panel1a-header"
        >
          <Typography>System {s}</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <div>Test</div>
          {console.log(dashboardPath+"?orgId=1&refresh="+refreshTime+"&from=now-"+timeRange+"&theme=light&panelId=12")}
          <iframe src={dashboardPath+"?orgId=1&refresh="+refreshTime+"&from=now-"+timeRange+"&theme=light&panelId=12"} width="100%" height="200px" frameBorder="0"/>
        </AccordionDetails>
      </Accordion>
    })}</div>}

  </div>
}
