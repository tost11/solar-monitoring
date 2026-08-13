import {BooleanStatus, setBooleanStatus} from "../api/SolarSystemAPI";
import Switch from "@mui/material/Switch";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import React from "react";

interface SetStatusListProps{
  booleanStatus: BooleanStatus[],
  internalSetBooleanStatus: (status:BooleanStatus[])=>void,
  internalDeleteBooleanStatus?: (systemId:string,status:BooleanStatus)=>void,
  loading: boolean,
  setLoading: (v:boolean)=>void,
  systemId: string,
  horizontal?: boolean
}

export default function SetStatusList({booleanStatus,setLoading,loading,internalDeleteBooleanStatus,internalSetBooleanStatus,systemId,horizontal}:SetStatusListProps) {

  const updateStatus = (systemId:string,status:BooleanStatus,newValue:boolean)=>{
    setLoading(true)
    setBooleanStatus(systemId,status.name,newValue).then((res)=>{
      let arr: BooleanStatus[] = []
      booleanStatus.forEach(v=>{
        if(v.name !== status.name){
          arr.push(v)
        }else{
          arr.push(res);
        }
      })
      internalSetBooleanStatus(arr)
      setLoading(false)
    }).catch(()=>{
      setLoading(false)
    })
  }

  return <div>
    {booleanStatus.length > 0 && <div style={{display: "flex",flexDirection: horizontal?"row":"column",flexWrap: horizontal?"wrap":undefined}}>
      {booleanStatus.map((v,i)=><div key={i}>
        <div style={{marginLeft: "10px",marginRight: "10px"}}>
          <Switch disabled={loading} checked={v.value} onChange={() => updateStatus(systemId,v,!v.value)}/>
          {v.name}
          {internalDeleteBooleanStatus &&
            <IconButton disabled={loading} onClick={()=>internalDeleteBooleanStatus(systemId,v)}><DeleteIcon/></IconButton>
          }
        </div>
      </div>)}
    </div>
    }

  </div>
}
