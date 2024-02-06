import React, {useEffect, useState} from "react";
import {getPublicSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import {Button, CircularProgress, Paper, Stack, styled} from "@mui/material";
import {useLocation, useNavigate} from "react-router-dom";
import {formatDefaultValueWithUnit} from "../Component/utils/GraphUtils";

export default function StartPage(){

  const [systems,setSystems] = useState<SolarSystemListDTO[]>()

  useEffect(()=>{
    getPublicSystems().then(res=>{
      setSystems(res)
    }
  )}
  ,[])

  const navigate = useNavigate()
  const location = useLocation()

  const Item = styled(Paper)(({ theme }) => ({
    backgroundColor: theme.palette.mode === 'dark' ? '#1A2027' : '#fff',
    ...theme.typography.body2,
    padding: theme.spacing(2)
  }));

  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>Solar Monitoring System Service Platform Whatever</h1>

    <div>Do you need a Webside where you can send your solar-system data to monitor them ? Then this here could be what you are looking for!</div>

    <h2>Access and registration</h2>
    <div>For now the registration is closed because the application is not in a final state, if you are still interested and like to be "test user" write me a mail: solar@tost-soft.de</div>

    <h2>Open Source</h2>
    <div>Found a bug or have some improvements checkout the GitHub Project <a href="https://github.com/tost11/solar-monitoring">here</a></div>

    <h2>Public Systems</h2>
    <p>Some Systems are Public and you can access them and view the data Check them out</p>
    {systems ? <div>

       <Stack spacing={1}>
         {systems.map((k,i)=>{
           return <Item key={i} className="defaultFlex">
             <b className={"marginCenterTopBottom"}>{k.name}</b>
             <div className={"marginCenterTopBottom"}>Type:{k.type}</div>
             {k.currentValues ? <>
                 <div className={"marginCenterTopBottom"} style={{color:"green"}}>Online</div>
                 {k.currentValues.inputWatt && <div className={"marginCenterTopBottom"}>{formatDefaultValueWithUnit(k.currentValues.inputWatt,"W",0)}</div>}
               </>:
             <div className={"marginCenterTopBottom"} style={{color:"red"}}>Offline</div>}
             <Button onClick={()=>navigate("/detailDashboard/"+k.id)}>To the Dashboard</Button>
             {(k.role=="Admin" || k.role=="Edit") &&
               <Button onClick={()=>navigate("/edit/System/"+k.id)}>
                 Edit System
               </Button>
             }
           </Item>
         })}
      </Stack>
      </div> :
      <div>
        <CircularProgress/>
      </div>
    }

  </div>
}
