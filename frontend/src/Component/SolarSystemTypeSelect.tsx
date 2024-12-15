import {TagDTO} from "../api/UserAPIFunctions";
import {SolarSystemType} from "../api/SolarSystemAPI";
import Select from "@mui/material/Select";
import {MenuItem} from "@mui/material";
import React from "react";
import DeleteForeverIcon from "@mui/icons-material/DeleteForever";


interface SolarSystemTypeSelectProps {
  selected?: SolarSystemType
  setSelected: (type?:SolarSystemType)=>void,
  renderClear?: boolean,
  fontSize: "inherit" | "large" | "medium" | "small"
}

export default function SolarSystemTypeSelect({selected,setSelected,renderClear,fontSize}: SolarSystemTypeSelectProps) {
  return <div style={{margin:"auto",display:"flex"}}>
    <Select
      style={{minWidth:"150px"}}
      value={selected}
      onChange={(ev)=>setSelected(ev.target.value as SolarSystemType)}
    >
      <MenuItem value={"SELFMADE"}>
        <div className="menuItem">Selfmade</div>
      </MenuItem>
      <MenuItem value={"SIMPLE"}>
        <div className="menuItem">Simple Solar System</div>
      </MenuItem>
      <MenuItem value={"VERY_SIMPLE"}>
        <div className="menuItem">Very Simple only Watt</div>
      </MenuItem>
      <MenuItem value={"GRID"}>
        <div className="menuItem">Grid Solar System</div>
      </MenuItem>
      <MenuItem value={"GRID_BATTERY"}>
        <div className="menuItem">Grid Solar System with Battery</div>
      </MenuItem>
    </Select>
    {renderClear && selected != undefined && <DeleteForeverIcon style={{padding: "2px", cursor: "pointer"}} onClick={() => setSelected(undefined)}
                                       fontSize={fontSize}/>}
  </div>
}
