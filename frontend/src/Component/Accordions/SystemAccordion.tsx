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
import {SolarSystemListDTO, SolarSystemSearchParams} from "../../api/SolarSystemAPI";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import CheckDeleteSystem from "../CheckDeleteSystem";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";
import {useTranslation} from "react-i18next";

interface AccordionProps {
  system:SolarSystemListDTO
  reloadSystems:()=>(searchParams: SolarSystemSearchParams) => void
  isInCompareList: boolean
  setInCompareList: (boolean)=>void
  key?: any
}


export default function SystemAccordion({key,style,system,reloadSystems,isInCompareList,setInCompareList}:AccordionProps) {

  const { t } = useTranslation();

  const [openDeleteCheck,setOpenDeleteCheck]=useState(false);
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

  return<div style={style} key={key}>
    <Accordion>
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <Typography component={'span'}>
        <div className={"flexColumn"}>
          <div className={"defaultFlex"} style={{margin:"auto",marginLeft:"10px",marginRight:"10px",fontSize:"18px"}}>
            <div>
              {system.name}
            </div>
            {system.currentValues ? <>
                <div style={{color:system.currentValues.inputWatt > 0 ? "green":"DarkOrange"}}>{t("common.online")}</div>
                {!(system.currentValues.inputWatt == undefined) && <div>{formatDefaultValueWithUnit(system.currentValues.inputWatt,"W",0)}</div>}
                {!(system.currentValues.batteryVoltage == undefined)&& <div>{formatDefaultValueWithUnit(system.currentValues.batteryVoltage,"V",2)}</div>}
                {!(system.totalProducedWH == undefined)&& <div>{formatDefaultValueWithUnit(system.totalProducedWH,"Wh", 2, true)}</div>}
              </>:
              <div style={{color:"red"}}>{t("common.offline")}</div>
            }
          </div>
          <div className={"defaultFlex"} style={{marginTop:"5px",margin:"auto",marginLeft:"10px",marginRight:"10px",fontSize:"18px"}} className={"defaultFlex"}>
            <Button variant="contained" onClick={()=>navigate("/dd/"+system.id)}>
              {t("views.systems_list.detail_view")}
            </Button>
            <Button style={{backgroundColor: isInCompareList?"lightblue":"whitesmoke"}} variant="outlined" onClick={(e)=>{
              e.stopPropagation()
              setInCompareList(!isInCompareList)
            }}>
              {t("common.compare")}
            </Button>
          </div>
        </div>
      </Typography>
    </AccordionSummary>
    <AccordionDetails>
      <Typography>
        Type: {system.type}
      </Typography>
      {system.role=="owns"&&
      <IconButton onClick={()=>setOpenDeleteCheck(true)}><DeleteIcon/></IconButton>
      }

    </AccordionDetails>
  </Accordion>
    <CheckDeleteSystem open={openDeleteCheck} onClose={closeDialogueAndReload} systemId={system.id}/>
  </div>
}
