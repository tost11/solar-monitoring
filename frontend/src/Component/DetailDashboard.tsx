import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {Box, Button, CircularProgress, FormControl, InputLabel} from "@mui/material";
import Select, { SelectChangeEvent } from '@mui/material/Select';
import {toast} from "react-toastify";
import MenuItem from '@mui/material/MenuItem';
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams } from "react-router-dom";
import SolarPanelAccordion from "./SolarPanelAccordion";
import BatteryAccordion from "./BatteryAccordion";
import DayAccordion from "./DayAccordion";



export default function DetailDashboardComponent() {
  const initialState = {
    name:"",
    creationDate:0,
    type:"",
    grafanaUid:"",
  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)


  const params = useParams()
  {/* TODO check if number*/
  }
  useEffect(() => {
   if(!isNaN(Number(params.id))){
    getSystem(""+params.id).then((res) => {
      setData(res)
    }).then(()=>
      setIsLoading(true))
  }}, [])
  const login = useContext(UserContext);
  const time = "30s";
  return <div>
    {isLoading ? <div>

      {data.type==="SELFMADE"&&<div>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>

      </div>}
      {data.type==="SELFMADE_CONSUMPTION"&&<div>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        {/*consumption*/}
      </div>}
      {data.type==="SELFMADE_INVERTER"&&<div>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
      </div>}
      {data.type==="SELFMADE_DEVICE"&&<div>
        <SolarPanelAccordion name={data.name} grafanaUid={data.grafanaUid}/>
        <BatteryAccordion name={data.name} grafanaUid={data.grafanaUid}/>
      </div>}

      <DayAccordion name={data.name} grafanaUid={data.grafanaUid}/>



    </div>:<CircularProgress/>}

  </div>
}
