import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button, Checkbox,
  Typography
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {SolarSystemListDTO} from "../../api/SolarSystemAPI";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import CheckDeleteSystem from "../CheckDeleteSystem";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";

interface AccordionProps {
  system:SolarSystemListDTO
  reloadSystems:()=>void
  isInCompareList: boolean
  setInCompareList: (boolean)=>void
}


export default function SystemAccordion({system,reloadSystems,isInCompareList,setInCompareList}:AccordionProps) {
  const [openDeleteCheck,setOpenDeleteCheck]=useState(false);
  const [isOpen,setIsOpen] =useState(false)
  if(system.type=="SELFMADE")
    system.type="Selfmade SolarSystem"
  if(system.type=="SELFMADE_CONSUMPTION")
    system.type="Selfmade with Consumption"
  if(system.type=="SELFMADE_INVERTER")
    system.type="Selfmade with inverter"
  if(system.type=="SELFMADE_DEVICE")
    system.type="Selfmade without converter"

  let navigate = useNavigate()

  const closeDialogueAndReload = () => {
    reloadSystems()
    setOpenDeleteCheck(false)
  }

  return<div>
    <Accordion>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
      onClick={()=>setIsOpen(!isOpen)}
    >
      <Typography>
        <div className={"defaultFlex"} style={{}}>
          <div style={{margin:"auto",marginLeft:"10px",marginRight:"10px",fontSize:"18px"}}>
            {system.name}
          </div>
          {system.currentValues ? <>
              <div style={{color:system.currentValues.inputWatt > 0 ? "green":"DarkOrange"}}>Online</div>
              {!(system.currentValues.inputWatt == undefined) && <div>{formatDefaultValueWithUnit(system.currentValues.inputWatt,"W",0)}</div>}
              {!(system.currentValues.batteryVoltage == undefined)&& <div>{formatDefaultValueWithUnit(system.currentValues.batteryVoltage,"V",2)}</div>}
            </>:
            <div style={{color:"red"}}>Offline</div>}
          <div className={"flexRow"} style={{borderRadius:"10px", backgroundColor: isInCompareList?"lightblue":"whitesmoke"}} onClick={e=>{
            e.stopPropagation()
            setInCompareList(!isInCompareList)
          }}>
            <div style={{margin:"auto",marginLeft:"10px",marginRight:"10px"}}>Compare</div>
          </div>
        </div>
      </Typography>
    </AccordionSummary>
    <AccordionDetails>
      <Typography>
        Type: {system.type}
      </Typography>
      <Button onClick={()=>navigate("/detailDashboard/"+system.id)}>
       To the Dashboard
      </Button>
      {system.role!="VIEW"&&
      <Button onClick={()=>navigate("/edit/System/"+system.id)}>
        Edit System
      </Button>
      }
      {system.role=="Admin"||system.role=="owns"&&
      <IconButton onClick={()=>setOpenDeleteCheck(true)}><DeleteIcon/></IconButton>
      }

    </AccordionDetails>
  </Accordion>
    <CheckDeleteSystem open={openDeleteCheck} onClose={closeDialogueAndReload} systemId={system.id}/>
  </div>
}
