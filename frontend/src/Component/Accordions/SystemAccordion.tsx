import {Accordion, AccordionDetails, AccordionSummary, Button, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {useNavigate} from "react-router-dom";

interface AccordionProps {
  id: number;
  name: string;
  type: string;
}


export default function SystemAccordion({id,name,type}:AccordionProps) {
  if(type=="SELFMADE")
    type="Selfmade SolarSystem"
  if(type=="SELFMADE_CONSUMPTION")
    type="Selfmade with Consumption"
  if(type=="SELFMADE_INVERTER")
    type="Selfmade with inverter"
  if(type=="SELFMADE_DEVICE")
    type="Selfmade without converter"

  let navigate = useNavigate()

  return<Accordion>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography>{name   +" id:"+ id}</Typography>
    </AccordionSummary>
    <AccordionDetails>

      <Typography>
        Type: {type}

      </Typography>
      <Button onClick={()=>navigate("/detailDashboard/"+id)}>
       To the Dashboard
      </Button>
      <Button >
        Edit System
      </Button>
    </AccordionDetails>
  </Accordion>
}
