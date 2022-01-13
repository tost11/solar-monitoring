import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {Box, Button, FormControl, InputLabel} from "@mui/material";
import Select, { SelectChangeEvent } from '@mui/material/Select';
import {toast} from "react-toastify";
import MenuItem from '@mui/material/MenuItem';
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams } from "react-router-dom";



export default function TestComponent() {
  const initialState = {
    name:"",
    creationDate:0,
    type:"",
    grafanaUid:"",
  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)
  const [isFrameLoading, setIsFrameLoading] = useState(true)
  const [age, setAge] = React.useState('');

  const handleChange = (event: SelectChangeEvent) => {
    setAge(event.target.value as string);
  };
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
    <Button
        onClick={() => {
          toast('🦄 Wow so easy!',{hideProgressBar:false})
        }}
    >Test Alert</Button>
    <br/>

    {isLoading ? <div>

      <iframe
        src={"/grafana/d-solo/"+data.grafanaUid+"/generated-"+data.name+"?orgId=1&refresh=10s&theme=light&panelId=0"} onLoad={()=>setIsFrameLoading(false)} width="450" height="200" frameBorder="0" hidden={isFrameLoading}/>
      <iframe
        src={"/grafana/d-solo/"+data.grafanaUid+"/generated-"+data.name+"?orgId=1&refresh=10s&theme=light&panelId=1"} onLoad={()=>setIsFrameLoading(false)} width="450" height="200" frameBorder="0" hidden={isFrameLoading}/>
      <iframe
        src={"/grafana/d-solo/"+data.grafanaUid+"/generated-"+data.name+"?orgId=1&refresh=10s&theme=light&panelId=2"} onLoad={()=>setIsFrameLoading(false)} width="450" height="200" frameBorder="0" hidden={isFrameLoading}/>
    </div>:<div>Loading....</div>}





  </div>
}
