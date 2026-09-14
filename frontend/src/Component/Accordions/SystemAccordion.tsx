import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Button from "@mui/material/Button";
import Typography from "@mui/material/Typography";
import Chip from "@mui/material/Chip";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import PersonIcon from "@mui/icons-material/Person";
import GroupIcon from "@mui/icons-material/Group";
import PublicIcon from "@mui/icons-material/Public";
import React, {useContext} from "react";
import {useNavigate} from "react-router-dom";
import {SolarSystemListDTO} from "../../api/SolarSystemAPI";
import {formatDefaultValueWithUnit} from "../utils/GraphUtils";
import {useTranslation} from "react-i18next";
import {UserContext} from "../../context/UserContext";
import moment from "moment";

interface AccordionProps {
  system:SolarSystemListDTO
  isInCompareList: boolean
  setInCompareList: (value: boolean)=>void
  style?: React.CSSProperties
  key?: any
}


export default function SystemAccordion({key,style,system,isInCompareList,setInCompareList}:AccordionProps) {

  const { t } = useTranslation();
  const login = useContext(UserContext);

  const getSystemTypeDisplay = (type: string) => {
    const typeKey = type.toLowerCase().replace(/_/g, '-');
    return t(`components.solarsystem.types.${typeKey}`, { defaultValue: type });
  }

  let navigate = useNavigate()

  const calculateAge = (creationDate: string): string => {
    const dateAsMoment = moment(creationDate);
    const now = moment();
    const days = now.diff(dateAsMoment, 'days');

    if (days < 365) {
      return `${days} ${t('components.system_specs_modal.days')}`;
    } else {
      const years = days / 365;
      return `${years.toFixed(1)} ${t('components.system_specs_modal.years')}`;
    }
  }

  const getSourceDisplay = () => {
    if (system.role == "owns") {
      return {icon: <PersonIcon fontSize="small"/>, label: t("views.systems_list.source.owned")};
    }
    if (system.role == "manages" || system.role == "view") {
      return {icon: <GroupIcon fontSize="small"/>, label: t("views.systems_list.source.shared")};
    }
    return {icon: <PublicIcon fontSize="small"/>, label: t("views.systems_list.source.public")};
  }

  const batteryValue = system.currentValues ? (
    system.currentValues.batteryPercentage != undefined
      ? formatDefaultValueWithUnit(system.currentValues.batteryPercentage,"%",0,true)
      : system.currentValues.batteryVoltage != undefined
        ? formatDefaultValueWithUnit(system.currentValues.batteryVoltage,"V",2)
        : undefined
  ) : undefined;

  return<div style={style} key={key}>
    <Accordion>
    <AccordionSummary
      component="div"
      expandIcon={<ExpandMoreIcon/>}
    >
      <Typography component={'span'}>
        <div className={"flexColumn"}>
          <div className={"defaultFlex"} style={{margin:"auto",marginLeft:"10px",marginRight:"10px",fontSize:"18px"}}>
            <div style={{display:"flex",alignItems:"center",gap:"10px"}}>
              <div>
                {system.name}
              </div>
              {login && <span title={getSourceDisplay().label}>{getSourceDisplay().icon}</span>}
            </div>
            {system.currentValues ? <>
                <div style={{color:system.currentValues.inputWatt > 0 ? "green":"DarkOrange"}}>{t("common.online")}</div>
                {!(system.currentValues.inputWatt == undefined) && <div>{formatDefaultValueWithUnit(system.currentValues.inputWatt,"W",0)}</div>}
                {batteryValue != undefined && <div>{batteryValue}</div>}
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
        {t("views.systems_list.type")}: {getSystemTypeDisplay(system.type)}
      </Typography>
      {system.maxInstalledSolarPower != undefined && (
        <Typography>
          {t("views.systems_list.max_installed_solar_power")}: {formatDefaultValueWithUnit(system.maxInstalledSolarPower, "W", 0, true)}
        </Typography>
      )}
      {system.maxInverterOutputPower != undefined && (
        <Typography>
          {t("views.systems_list.max_inverter_output_power")}: {formatDefaultValueWithUnit(system.maxInverterOutputPower, "W", 0, true)}
        </Typography>
      )}
      {system.batteryCapacity != undefined && (
        <Typography>
          {t("components.system_specs_modal.battery_capacity")}: {formatDefaultValueWithUnit(system.batteryCapacity, "kWh", 1, true)}
        </Typography>
      )}
      {system.totalProducedWH != undefined && (
        <Typography>
          {t("views.systems_list.total_produced")}: {formatDefaultValueWithUnit(system.totalProducedWH / 1000, "kWh", 1, true)}
        </Typography>
      )}
      {system.buildingDate && moment(system.buildingDate).isValid() && (
        <Typography>
          {t("common.building_date")}: {moment(system.buildingDate).format("YYYY-MM-DD")}
        </Typography>
      )}
      {system.creationDate && moment(system.creationDate).isValid() && (
        <Typography>
          {t("components.system_specs_modal.system_age")}: {calculateAge(system.creationDate)}
        </Typography>
      )}
      {system.currentValues && (
        <>
          {system.currentValues.outputWatt != undefined && (
            <Typography>
              {t("views.systems_list.consumption")}: {formatDefaultValueWithUnit(system.currentValues.outputWatt, "W", 0)}
            </Typography>
          )}
          {system.currentValues.gridWatt != undefined && (
            <Typography>
              {t("common.grid")}: {formatDefaultValueWithUnit(system.currentValues.gridWatt, "W", 0)}
            </Typography>
          )}
          {system.currentValues.batteryWatt != undefined && (
            <Typography>
              {t("common.battery")}: {formatDefaultValueWithUnit(system.currentValues.batteryWatt, "W", 0)}
            </Typography>
          )}
        </>
      )}
      {system.tags && system.tags.length > 0 && (
        <div style={{marginTop:"10px",display:"flex",gap:"8px",flexWrap:"wrap"}}>
          {system.tags.map((tag) => (
            <Chip
              key={tag.id}
              label={tag.name}
              size="medium"
              clickable
              style={{backgroundColor: tag.color, color: "white"}}
              onClick={()=>navigate("/tag/"+tag.id)}
            />
          ))}
        </div>
      )}
    </AccordionDetails>
  </Accordion>
  </div>
}
