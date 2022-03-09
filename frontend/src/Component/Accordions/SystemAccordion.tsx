import {Accordion, AccordionDetails, AccordionSummary, Button, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {SolarSystemListDTO} from "../../api/SolarSystemAPI";

interface AccordionProps {
  system:SolarSystemListDTO
}


export default function SystemAccordion({system}:AccordionProps) {
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


  return<Accordion>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
      onClick={()=>setIsOpen(!isOpen)}
    >
      <Typography>{system.name   +" id:"+ system.id}</Typography>
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

    </AccordionDetails>
  </Accordion>
}
