import React, {useEffect, useState} from "react";
import {Accordion, AccordionDetails, AccordionSummary, CircularProgress, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {SolarSystemDashboardDTO} from "../../api/SolarSystemAPI";
import moment from "moment";
import ShowTimePickerComponent from "../ShowTimePickerComponent";
import {getStatisticGraphData} from "../../api/GraphAPI";
import BarGraph from "../BarGraph";
import {GraphDataObject} from "../DetailDashboard";

interface AccordionProps {
  systemInfo: SolarSystemDashboardDTO;
}
export type DashboardRange = "1w" | "1M" | "1y";

export default function StatisticsAccordion({systemInfo}: AccordionProps) {
  const [loading, setLoading] = useState(false)
  const [selectDashboard,setSelectDashboard] = useState("1w")
  const [selectDate, setSelectDate] = useState<null|number>(null);
  const [fromDate,setFromDate]=useState<null|number>(null);
  const [isOpen, setIsOpen] = useState(false)
  const [graphData,setGraphData] =useState<GraphDataObject>()




useEffect(()=>{
  setLoading(false)
  if(selectDate!=null) {
    if (selectDashboard === "1w")
      setFromDate(selectDate - 604800000)
    if (selectDashboard === "1M")
      setFromDate(selectDate - 2674800000)
    if (selectDashboard === "1y")
      setFromDate(selectDate - 31532400000)
  }
  else {
    setSelectDate((moment().valueOf()))
  }
},[selectDashboard,selectDate])

  useEffect(()=>{
    if(fromDate!=null&&selectDate!=null) {
      getStatisticGraphData(systemInfo.id, fromDate, selectDate).then((r)=>{
        setGraphData({data:r})
        setLoading(true)
      })
    }
  },[fromDate])

  return <Accordion style={{backgroundColor:"Lavender"}} className={"DetailAccordion"} >
    <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
    >
      <Typography>Statistics</Typography>
    </AccordionSummary>


    <AccordionDetails>
      <ShowTimePickerComponent creationDate={systemInfo.creationDate} setSelectDashboard={(s:DashboardRange)=>{setSelectDashboard(s)}} setSelectDate={(a:number)=>setSelectDate(a)}/>
              <div className="defaultFlowColumn">
                <div style={{margin:"5px",display: "flex",flexDirection: "column"}}>
                  {selectDashboard&&graphData&&fromDate&&selectDate&&<div>
                    <BarGraph from={fromDate} to={selectDate} graphData={graphData} labels={["Produce","Consumption"]}/>
                    <BarGraph  from={fromDate} to={selectDate} graphData={graphData} labels={["Difference"]}/>
                  </div>}
                </div>
            </div>
    </AccordionDetails>
  </Accordion>
}
