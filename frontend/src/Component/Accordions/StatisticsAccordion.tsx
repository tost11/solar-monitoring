import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import ShowTimePickerComponent from "../ShowTimePickerComponent"
import moment from "moment";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
  dashboardPath: String;
}
export type DashboardRange = "Week" | "Month" | "Year";

export default function StatisticsAccordion({systemInfo,dashboardPath}: AccordionProps) {
  const [panel1Loading, setPanel1Loading] = useState(true)
  const [panel2Loading, setPanel2Loading] = useState(true)
  const [panel3Loading, setPanel3Loading] = useState(true)
  const [selectDashboard,setSelectDashboard] = useState("Week")
  const [selectDate, setSelectDate] = useState<null|number>(null);
  const [fromDate,setFromDate]=useState<null|number>(null);
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

  if(selectDate!=null) {
    if (selectDashboard === "Week")
      setFromDate(selectDate - 604800000)
    if (selectDashboard === "Month")
      setFromDate(selectDate - 2674800000)
    if (selectDashboard === "Year")
      setFromDate(selectDate - 31532400000)
  }
  else {
    setSelectDate((moment().valueOf()))
  }
},[selectDashboard,selectDate])

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
    >
      <Typography>Statistics</Typography>
    </AccordionSummary>


    <AccordionDetails>
      <ShowTimePickerComponent creationDate={systemInfo.creationDate} setSelectDashboard={(s:DashboardRange)=>{setSelectDashboard(s)}} setSelectDate={(a:number)=>setSelectDate(a)}/>

      {isOpen && <div>
        {isLoading() && <CircularProgress/>}
            <div style={isLoading()?{display:'none'}:{}}>
            <div className="defaultFlowColumn">
              <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>

                {selectDashboard==="Week"&&<div>

                <iframe
                src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=13&from="+fromDate}
                onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=14&from="+fromDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=15&from="+fromDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
                </div>}
              {selectDashboard==="Month"&&<div>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=16&from="+fromDate}
                  onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=17&from="+fromDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=18&from="+fromDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
              </div>}
              {selectDashboard==="Year"&&<div>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=19&from="+fromDate}
                  onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=20&from="+fromDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+selectDate+"&panelId=21&from="+fromDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
              </div>}
              </div>
            </div>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
