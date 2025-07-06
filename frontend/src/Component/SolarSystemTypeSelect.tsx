import {TagDTO} from "../api/UserAPIFunctions";
import {SolarSystemType} from "../api/SolarSystemAPI";
import Select from "@mui/material/Select";
import {MenuItem} from "@mui/material";
import React from "react";
import DeleteForeverIcon from "@mui/icons-material/DeleteForever";
import {useTranslation} from "react-i18next";


interface SolarSystemTypeSelectProps {
  selected?: SolarSystemType
  setSelected: (type?:SolarSystemType)=>void,
  renderClear?: boolean,
  fontSize: "inherit" | "large" | "medium" | "small",
  preferredWidth?: string
}

export default function SolarSystemTypeSelect({preferredWidth,selected,setSelected,renderClear,fontSize}: SolarSystemTypeSelectProps) {

  var { t } = useTranslation();

  return <div style={{margin:"auto",display:"flex"}}>
    <Select
      style={{minWidth:"100px",maxWidth:preferredWidth}}
      value={selected}
      onChange={(ev)=>setSelected(ev.target.value as SolarSystemType)}
    >
      <MenuItem value={"SELFMADE"}>
        <div className="menuItem">{t("components.solarsystem.types.selfmade")}</div>
      </MenuItem>
      <MenuItem value={"SIMPLE"}>
        <div className="menuItem">{t("components.solarsystem.types.simple")}</div>
      </MenuItem>
      <MenuItem value={"VERY_SIMPLE"}>
        <div className="menuItem">{t("components.solarsystem.types.very-simple")}</div>
      </MenuItem>
      <MenuItem value={"GRID"}>
        <div className="menuItem">{t("components.solarsystem.types.grid")}</div>
      </MenuItem>
      <MenuItem value={"GRID_BATTERY"}>
        <div className="menuItem">{t("components.solarsystem.types.grid")}</div>
      </MenuItem>
    </Select>
    {renderClear && selected != undefined && <DeleteForeverIcon style={{marginTop:"auto",marginBottom:"auto",padding: "2px", cursor: "pointer"}} onClick={() => setSelected(undefined)}
                                       fontSize={fontSize}/>}
  </div>
}
