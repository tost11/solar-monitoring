import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  Typography,
  IconButton
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import React, {useState} from "react";
import {useNavigate} from "react-router-dom";
import {SolarSystemListDTO, SolarSystemSearchParams} from "../../api/SolarSystemAPI";
import DeleteIcon from "@mui/icons-material/Delete";
import CheckDeleteSystem from "../CheckDeleteSystem";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";
import {useTranslation} from "react-i18next";

interface AccordionProps {
  system:SolarSystemListDTO
  reloadSystems:()=>(searchParams: SolarSystemSearchParams) => void
  isInCompareList: boolean
  setInCompareList: (value: boolean)=>void
  style?: React.CSSProperties
  key?: any
}


export default function SystemAccordion({key,style,system,reloadSystems,isInCompareList,setInCompareList}:AccordionProps) {

  const { t } = useTranslation();

  const [openDeleteCheck,setOpenDeleteCheck]=useState(false);

  const getSystemTypeDisplay = (type: string) => {
    const typeKey = type.toLowerCase().replace(/_/g, '-');
    return t(`components.solarsystem.types.${typeKey}`, { defaultValue: type });
  }

  let navigate = useNavigate()

  const closeDialogueAndReload = () => {
    reloadSystems()
    setOpenDeleteCheck(false)
  }

  return<div style={style} key={key}>
    <Accordion>
    <AccordionSummary
      component="div"
      expandIcon={<ExpandMoreIcon/>}
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
          <div className={"defaultFlex"} style={{marginTop:"5px",margin:"auto",marginLeft:"10px",marginRight:"10px",fontSize:"18px"}}>
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
        Type: {getSystemTypeDisplay(system.type)}
      </Typography>
      {system.maxInstalledSolarPower && (
        <Typography>
          {t("views.systems_list.max_installed_solar_power")}: {formatDefaultValueWithUnit(system.maxInstalledSolarPower, "W", 0, true)}
        </Typography>
      )}
      {system.maxInverterOutputPower && (
        <Typography>
          {t("views.systems_list.max_inverter_output_power")}: {formatDefaultValueWithUnit(system.maxInverterOutputPower, "W", 0, true)}
        </Typography>
      )}
      {system.role=="owns"&&
      <IconButton onClick={()=>setOpenDeleteCheck(true)}><DeleteIcon/></IconButton>
      }

    </AccordionDetails>
  </Accordion>
    <CheckDeleteSystem open={openDeleteCheck} onClose={closeDialogueAndReload} systemId={system.id}/>
  </div>
}
