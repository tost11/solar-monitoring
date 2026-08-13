import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
import React from "react";
import {useTranslation} from "react-i18next";

interface CheckBoxComponentFiltersProps {
  systemIds: string[],
  namings: {[key: string]: string},
  getSystemColour: (v:string)=>string,
  checkSystemIds: Set<string>,
  setCheckedSystemIds: (v:Set<string>)=>void
}

export default function CheckBoxComponentFilters({systemIds,checkSystemIds,setCheckedSystemIds,
                                                   namings,getSystemColour}:CheckBoxComponentFiltersProps){

  var { t } = useTranslation();

  const changeIdSelection = (id:string,on:Set<string>,set:(v:Set<string>)=>void)=>{
    //@ts-ignore
    var newSelection = new Set<string>(on)
    if(newSelection.has(id)){
      newSelection.delete(id)
    }else{
      newSelection.add(id)
    }
    set(newSelection)
  }

  const getNameOrFallbackId = (id:string,map:{[key: string]: string})=>{
    let name = map[id]
    if(name){
      return name
    }
    let arr = id.split("-")
    if(arr.length > 1){
      return arr[1];
    }
    return id
  }

  return <div>
    <h4>{t("components.solarsystem.compare")}</h4>
    <div className="defaultFlex" style={{justifyContent:"center"}}>
      {Object.entries(systemIds).map(([_k,v],i)=> {
        return <div style={{margin:"auto",backgroundColor:"white",padding: "5px 10px 5px 10px",borderRadius: "6px"}} key={i}>
          <FormControlLabel
            label={<div style={{color: getSystemColour(v)}}>{t("common.system")+" " + getNameOrFallbackId(v,namings)}</div>}
            control={<Checkbox
              checked={checkSystemIds.has(v)}
              onChange={() => changeIdSelection(v, checkSystemIds, setCheckedSystemIds)}
              slotProps={{ input: { 'aria-label': 'controlled' } }}
          />}
        />
      </div>})}
    </div>
  </div>
}
