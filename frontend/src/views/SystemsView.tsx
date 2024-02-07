import React, {useContext, useEffect, useState} from "react";
import {getPublicSystems, getSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import SystemAccordion from "../Component/Accordions/SystemAccordion";
import {Button, Switch, Typography} from "@mui/material";
import {useNavigate} from "react-router-dom";
import {UserContext} from "../context/UserContext";
import {setSectionValue} from "@mui/x-date-pickers/internals/hooks/useField/useField.utils";

export default function SystemsView() {

  const [data, setData] = useState<SolarSystemListDTO[]>([])
  const [compareMap,setCompareMap] = useState(new Map<string,string>)
  const [showPublicSystems,setShowPublicSystems] = useState<boolean>(false)

  const navigate = useNavigate();

  const login = useContext(UserContext);

  const crateNavigationParams = (map:Map<string,string>)=>{
    let ret = ""
    map.forEach((v,k)=>{
      if(ret.length != 0){
        ret += "&"
      }
      if(v != undefined){
        ret += "sys="+v
      }else{
        ret += "sys="+k
      }
    })
    return ret;
  }

  const reloadSystems = () => {
    if(login == null){
      getPublicSystems().then((res) => {
        setData(res)
      })
    }else{
      getSystems(showPublicSystems).then((res) => {
        setData(res)
      })
    }
  }
  useEffect(()=>reloadSystems(),[showPublicSystems])
  return <div>

    {data.length > 0 ? <>
      {login && <div style={{marginTop:"10px"}}>
        <Typography>
          <Switch checked={showPublicSystems} onChange={()=>{
            setShowPublicSystems(!showPublicSystems)
            setCompareMap(new Map())
          }}/>
            Also show public systems
          </Typography>
       </div>}
      {data.map((e,i)=>
        <div style={{marginTop: "7px"}}>
          <SystemAccordion style={{padding: "10px"}} isInCompareList={compareMap.has(e.id)} key={i} system={e} reloadSystems={reloadSystems} setInCompareList={sel=>{
            if(sel) {
              let v = new Map(compareMap)
              v.set(e.id,e.shortener);
              setCompareMap(v)
            }else {
              let v = new Map(compareMap)
              v.delete(e.id)
              setCompareMap(v)
            }
          }}/>
        </div>)}
        <br/>
        {compareMap.size} Systems selected<br/>
          <Button disabled={compareMap.size<2} variant="contained" style={{marginTop: "20px"}} onClick={e=>{
          navigate("/compare?"+crateNavigationParams(compareMap))
        }}>Compare Selected Systems</Button>
      </>:
      <div style={{marginTop:"10px"}}>
        No Systems available
      </div>
    }

  </div>
}

