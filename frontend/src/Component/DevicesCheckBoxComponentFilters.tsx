import {Checkbox, Divider, FormControlLabel} from "@mui/material";
import {getGraphColourByIndex} from "./utils/GraphUtils";
import React from "react";
import {DeviceIdsWrapper} from "../api/GraphAPI";
import {NamingsDTO} from "../api/SolarSystemAPI";

interface DevicesCheckBoxComponentFiltersProps {
  devices: DeviceIdsWrapper,
  showCombined: boolean,
  setShowCombined: (v:boolean)=>void,
  getDeviceColour: (v:string)=>string,
  checkedDeviceIds: Set<string>,
  checkedInputDCIds: Set<string>,
  checkedInputACIds: Set<string>,
  checkedOutputDCIds: Set<string>,
  checkedOutputACIds: Set<string>,
  checkedBatteryIds: Set<string>,
  setCheckedDeviceIds: (v:Set<string>)=>void,
  setCheckedInputDCIds: (v:Set<string>)=>void,
  setCheckedInputACIds: (v:Set<string>)=>void,
  setCheckedOutputDCIds: (v:Set<string>)=>void,
  setCheckedOutputACIds: (v:Set<string>)=>void,
  setCheckedBatteryIds: (v:Set<string>)=>void,
  namings:NamingsDTO
}

export default function DevicesCheckBoxComponentFilters({devices,showCombined,setShowCombined,getDeviceColour,checkedDeviceIds,checkedInputDCIds,checkedInputACIds,checkedOutputDCIds,checkedOutputACIds,checkedBatteryIds,
                                                   setCheckedDeviceIds,setCheckedInputDCIds,setCheckedInputACIds,setCheckedOutputDCIds,setCheckedOutputACIds,setCheckedBatteryIds,namings}:DevicesCheckBoxComponentFiltersProps){

  const changeIdSelection = (id:string,on:Set<string>,set:(v:Set<string>)=>void)=>{
    var newSelection = new Set<string>(on)
    if(newSelection.has(id)){
      newSelection.delete(id)
    }else{
      newSelection.add(id)
    }
    set(newSelection)
  }

  const showCombinedBox = ()=>{
    if(Object.entries(devices).length > 1){
      return true;
    }

    let dev = null
    Object.entries(devices).forEach(([k,v])=>{
      dev = v;
    });
    if(dev != null) {
      // @ts-ignore
      return dev.inputDCIds.length + dev.inputACIds.length  + dev.batteryIds.length  + dev.outputDCIds.length  + dev.outputDCIds.length  > 1;
    }
    return false;
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
    <h4>Possible Devices</h4>
    {showCombinedBox() && <FormControlLabel
        label={<div style={{color:getGraphColourByIndex(0)}}>Combined</div>}
        control={<Checkbox
            checked={showCombined}
            onChange={()=>setShowCombined(!showCombined)}
            inputProps={{ 'aria-label': 'controlled' }}
        />}
    />}
    <div className="defaultFlex" style={{justifyContent:"center"}}>
      {Object.entries(devices).map(([k,v],i)=> {
        return <div style={{margin:"auto",backgroundColor:"white",padding: "5px 10px 5px 10px",borderRadius: "6px"}} key={i}>
          <FormControlLabel
            label={<div style={{color: getDeviceColour("d-" + k)}}>{"Device " + getNameOrFallbackId(k,namings.devices)}</div>}
            control={showCombinedBox() ? <Checkbox
              checked={checkedDeviceIds.has("" + k)}
              onChange={() => changeIdSelection(k, checkedDeviceIds, setCheckedDeviceIds)}
              inputProps={{'aria-label': 'controlled'}}
          />:<div style={{marginLeft: "10px"}}/>}
        />
        <Divider />
        <div style={{display:"flex", flexWrap: "wrap" , maxWidth: "500px"}}>
          {v.inputDCIds.map((id,i2)=>{
            return <FormControlLabel
              key={i2}
              label={<div style={{color: getDeviceColour("i-"+k+"-"+id)}}>{"Input "+getNameOrFallbackId(k+"-"+id,namings.inputsDC) +" (DC)"}</div>}
              control={<Checkbox
                checked={checkedInputDCIds.has(""+k+"-"+id)}
                onChange={()=>changeIdSelection(""+k+"-"+id,checkedInputDCIds,setCheckedInputDCIds)}
                inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          })}
          {v.inputACIds.map((id,i2)=>{
            return <FormControlLabel
              key={i2}
              label={<div style={{color: getDeviceColour("j-"+k+"-"+id)}}>{"Input "+getNameOrFallbackId(k+"-"+id,namings.inputsAC)+" (AC)"}</div>}
              control={<Checkbox
                checked={checkedInputACIds.has(""+k+"-"+id)}
                onChange={()=>changeIdSelection(""+k+"-"+id,checkedInputACIds,setCheckedInputACIds)}
                inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          })}
          {v.batteryIds.map((id,i2)=>{
            return <FormControlLabel
                key={i2}
                label={<div style={{color: getDeviceColour("b-"+k+"-"+id)}}>{"Battery "+getNameOrFallbackId(k+"-"+id,namings.batteries) }</div>}
                control={<Checkbox
                    checked={checkedBatteryIds.has(""+k+"-"+id)}
                    onChange={()=>changeIdSelection(""+k+"-"+id,checkedBatteryIds,setCheckedBatteryIds)}
                    inputProps={{ 'aria-label': 'controlled' }}
                />}
            />
          })}
          {v.outputDCIds.map((id,i2)=>{
            return <FormControlLabel
              key={i2}
              label={<div style={{color: getDeviceColour("o-"+k+"-"+id)}}>{"Output "+getNameOrFallbackId(k+"-"+id,namings.outputsDC)+" (DC)"}</div>}
              control={<Checkbox
                checked={checkedOutputDCIds.has(""+k+"-"+id)}
                onChange={()=>changeIdSelection(""+k+"-"+id,checkedOutputDCIds,setCheckedOutputDCIds)}
                inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          })}
          {v.outputACIds.map((id,i2)=>{
            return <FormControlLabel
              key={i2}
              label={<div style={{color: getDeviceColour("c-"+k+"-"+id)}}>{"Output "+getNameOrFallbackId(k+"-"+id,namings.outputsAC)+" (AC)"}</div>}
              control={<Checkbox
                checked={checkedOutputACIds.has(""+k+"-"+id)}
                onChange={()=>changeIdSelection(""+k+"-"+id,checkedOutputACIds,setCheckedOutputACIds)}
                inputProps={{ 'aria-label': 'controlled' }}
              />}
            />
          })}
        </div>
      </div>})}
    </div>
  </div>
}
