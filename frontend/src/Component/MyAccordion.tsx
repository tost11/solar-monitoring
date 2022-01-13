import {Accordion, AccordionDetails, AccordionSummary, Button, IconButton, Typography} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React from "react";
import {SolarSystemListDTO} from "../api/SolarSystemAPI";
import InfoIcon from "@mui/icons-material/Info";
import {useNavigate} from "react-router-dom";

interface AccordionProps {
  id: number;
  name: string;
  type: string;

}


export default function MyAccordion({id,name,type}:AccordionProps) {
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
      <Button >
       To the Dashboard
      </Button>
      <Button onClick={()=>navigate("/test/"+id)}>
        Edit System
      </Button>
    </AccordionDetails>
  </Accordion>
}
