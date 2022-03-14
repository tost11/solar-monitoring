import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import moment from "moment";
import ShowTimePickerComponent from "../ShowTimePickerComponent";

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
  const [selectDate, setSelectDate] = useState<number>(new Date().getTime());
  const [toDate,setToDate]=useState<number>(selectDate+604800000);
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


  const selectNewDate = (newDate:number)=>{
    setPanel1Loading(true)
    setPanel2Loading(true)
    setPanel3Loading(true)

    if(newDate!=null) {
      if (selectDashboard === "Week") {
        setToDate(newDate + 604800000)
      }
      if (selectDashboard === "Month") {
        setToDate(newDate + 2674800000)
      }
      if (selectDashboard === "Year") {
        setToDate(newDate + 31532400000)
      }
    }
    setSelectDate(newDate)
  }

  const dashboardTimeRangeChanges = (newTimeRange:string)=>{
    setPanel1Loading(true)
    setPanel2Loading(true)
    setPanel3Loading(true)

    if(selectDate!=null) {
      if (newTimeRange === "Week") {
        setToDate(selectDate + 604800000)
      }
      if (newTimeRange === "Month") {
        setToDate(selectDate + 2674800000)
      }
      if (newTimeRange === "Year") {
        setToDate(selectDate + 31532400000)
      }
    }

    setSelectDashboard(newTimeRange)
  }


  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} onChange={changePanelStatus}>
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
    >
      <Typography>Statistics</Typography>
    </AccordionSummary>


    <AccordionDetails>
      <ShowTimePickerComponent creationDate={systemInfo.creationDate} setSelectDashboard={dashboardTimeRangeChanges} setSelectDate={selectNewDate} />
      {isOpen && <div>
        {isLoading() && <CircularProgress/>}
            <div style={isLoading()?{display:'none'}:{}}>
            <div className="defaultFlowColumn">
              <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>

                {selectDashboard==="Week"&&<div>

                <iframe
                src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=13&from="+selectDate}
                onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=14&from="+selectDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=15&from="+selectDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
                </div>}
              {selectDashboard==="Month"&&<div>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=16&from="+selectDate}
                  onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=17&from="+selectDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=18&from="+selectDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
              </div>}
              {selectDashboard==="Year"&&<div>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=19&from="+selectDate}
                  onLoad={()=>setPanel1Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=20&from="+selectDate}
                  onLoad={()=>setPanel2Loading(false)} width="100%" height="200px" frameBorder="0"/>
                <iframe
                  src={dashboardPath+"?orgId=1&refresh=1h&theme=light&to="+toDate+"&panelId=21&from="+selectDate}
                  onLoad={()=>setPanel3Loading(false)} width="100%" height="200px" frameBorder="0"/>
              </div>}
              </div>
            </div>
          </div>
        </div>}
    </AccordionDetails>
  </Accordion>
}
