
import {SolarSystemType} from "../api/SolarSystemAPI";
import {Select, MenuItem} from "@mui/material";
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
      <MenuItem value={"SELFMADE"} sx={{ whiteSpace: 'pre-wrap' }}>
        {t("components.solarsystem.types.selfmade")}
      </MenuItem>
      <MenuItem value={"SIMPLE"} sx={{ whiteSpace: 'pre-wrap' }}>
        {t("components.solarsystem.types.simple")}
      </MenuItem>
      <MenuItem value={"VERY_SIMPLE"} sx={{ whiteSpace: 'pre-wrap' }}>
        {t("components.solarsystem.types.very_simple")}
      </MenuItem>
      <MenuItem value={"GRID"} sx={{ whiteSpace: 'pre-wrap' }}>
        {t("components.solarsystem.types.grid")}
      </MenuItem>
      <MenuItem value={"GRID_BATTERY"} sx={{ whiteSpace: 'pre-wrap' }}>
        {t("components.solarsystem.types.grid_battery")}
      </MenuItem>
    </Select>
    {renderClear && selected != undefined && <DeleteForeverIcon style={{marginTop:"auto",marginBottom:"auto",padding: "2px", cursor: "pointer"}} onClick={() => setSelected(undefined)}
                                       fontSize={fontSize}/>}
  </div>
}
