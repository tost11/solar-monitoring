import TextField from "@mui/material/TextField";

import {Button} from "@mui/material";
import {useEffect} from "react";
import React from "react";

export interface NamingsManagerProps {
  setNamings: (v: Map<number,string>) => void
  namings: Map<number,string>;
}

export default function NamingsManager({namings,setNamings}: NamingsManagerProps) {

  const addNaming = ()=>{
    let ret = new Map(Array.from(namings))
    ret.set(-1,"")
    setNamings(ret)
  };

  const idChanged = (idOld:number,idNew:number)=>{
    console.log("fdfdsf",idOld,idNew)
    let ret = new Map(Array.from(namings))
    var value = ret.get(idOld)
    ret.delete(idOld)
    ret.set(idNew,value)
    setNamings(ret)
  };

  useEffect(()=> {
    console.log(namings)
  },[namings])


  return <>
    <Button onClick={addNaming} variant="contained">Add Naming</Button>
    {Array.from(namings).map(([k, v]) => <div className="defaultFlex">
        <TextField className={"Input default-margin"} label="Id" variant="outlined" type={"number"} value={k} onChange={e=>idChanged(k,e.target.value)}/>
        <TextField className={"Input default-margin"} label="Name" variant="outlined" value={v}/>
    </div>)}
    </>

}