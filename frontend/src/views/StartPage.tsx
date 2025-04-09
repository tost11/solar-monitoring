import React, {useEffect, useState} from "react";
import {
  getSystemsByTag, SolarSystemListDTO,
  TagSolarSystemDTO
} from "../api/SolarSystemAPI";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  CircularProgress, Divider,
  Stack
} from "@mui/material";
import {useLocation, useNavigate} from "react-router-dom";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import Button from "@mui/material/Button";
import {formatDefaultValueWithUnit} from "../Component/utils/GraphUtils";

const getOnlineSystems = (systems:SolarSystemListDTO[])=>{
  var count = 0;
  for (let system of systems) {
    if(system.currentValues){
      count ++;
    }
  }
  return count;
}

const getTotalProduction = (systems:SolarSystemListDTO[])=>{
  var total = 0;
  for (let system of systems) {
      if(system.currentValues && system.currentValues.inputWatt) {
        total = total + system.currentValues.inputWatt;
      }
  }
  return total;
}

const getTotalProducedWH = (systems:SolarSystemListDTO[])=>{
  var total = 0;
  for (let system of systems) {
    if(system.totalProducedWH){
        total = total + system.totalProducedWH;
    }
  }
  return total;
}

const crateNavigationParams = (systems:SolarSystemListDTO[])=>{
  let ret = ""
  for (let system of systems) {
    if(ret.length != 0) {
      ret += "&"
    }
    ret += "sys="+system.id
  }
  return ret;
}

function RenderTagSystemsAccordion({key,tagSolarSystems: tagSolarSystemDTO}){

  const navigate = useNavigate()

  const numOnline = getOnlineSystems(tagSolarSystemDTO.systems);
  const dif = numOnline / tagSolarSystemDTO.systems.length;
  const totalInputWatt = getTotalProduction(tagSolarSystemDTO.systems);
  const totalProducedWH = getTotalProducedWH(tagSolarSystemDTO.systems);

  return <Accordion key={key} defaultExpanded="true">
    <AccordionSummary
      expandIcon={<ExpandMoreIcon/>}
      aria-controls="panel1a-content"
      id="panel1a-header"
    >
      <div className="defaultFlex" style={{fontSize: "18px"}}>
        Systems with tag:
        <span style={{display:"flex",flexWrap:"wrap",backgroundColor:tagSolarSystemDTO.tag.color,paddingLeft:"15px",paddingRight:"15px",borderRadius:"20px"}}>{tagSolarSystemDTO.tag.name}</span>
        <span>Online: <span style={{color:dif <= 0 ? "red": dif >= 1 ? "green" : "orange"}}>{numOnline}</span>/{tagSolarSystemDTO.systems.length}</span>
        {totalInputWatt > 0 && <span>Current Power: {formatDefaultValueWithUnit(totalInputWatt,"W",0)}</span>}
        <span>Total Output: {formatDefaultValueWithUnit(totalProducedWH,"Wh", 2, true)}</span>
        {tagSolarSystemDTO.systems.length > 1 && <span>
            <Button style={{paddingTop:"0px",paddingBottom:"0px"}} onClick={()=>{navigate("/compare?"+crateNavigationParams(tagSolarSystemDTO.systems))
          }}>Compare Systems</Button>
        </span>}
      </div>
    </AccordionSummary>
    <AccordionDetails style={{backgroundColor:"floralwhite"}}>
      {tagSolarSystemDTO.systems.map((k, i) => {
        return <>
          <Divider style={{marginTop:"2px"}}/>
          <div style={{margin:"5px"}} key={i} className="defaultFlex">
            <b className={"marginCenterTopBottom"}>{k.name}</b>
            <div className={"marginCenterTopBottom"}>Type:{k.type}</div>
            {k.currentValues ? <>
                <div className={"marginCenterTopBottom"} style={{color: k.currentValues.inputWatt > 0 ? "green" : "DarkOrange"}}>Online</div>
                {k.currentValues.inputWatt !== undefined && <div
                  className={"marginCenterTopBottom"}>{formatDefaultValueWithUnit(k.currentValues.inputWatt, "W", 0)}</div>}
              </> :
              <div className={"marginCenterTopBottom"} style={{color: "red"}}>Offline</div>}
            <Button onClick={()=>navigate("/dd/"+k.id)}>To the Dashboard</Button>
            {(k.role=="Admin" || k.role=="Edit") &&
              <Button onClick={()=>navigate("/edit/System/"+k.id)}>
                Edit System
              </Button>
            }
          </div>
          <Divider/>
        </>}
      )}
    </AccordionDetails>
  </Accordion>
}

export default function StartPage(){

  const [systemsByTag,setSystemsByTag] = useState<TagSolarSystemDTO[]>()

  useEffect(()=>{
      getSystemsByTag().then(res=>{
        setSystemsByTag(res)
    }
  )}
  ,[])

  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>Solar Monitoring System Service Platform Whatever</h1>

    <div>Do you need a website where you can send your solar-system data to monitor them ? Then this here could be what you are looking for!</div>

    <h2>Access and registration</h2>
    <div>For now the registration is closed because the application is not in a final state, if you are still interested and like to be "test user" write me a mail: solar@tost-soft.de</div>

    <h2>Open Source</h2>
    <div>Found a bug or have some improvements checkout the GitHub Project <a href="https://github.com/tost11/solar-monitoring">here</a></div>

    <h2>Public Systems</h2>
    <p>Some Systems are Public and you can access them and view the data Check them out</p>
    {systemsByTag ? <div>

       <Stack spacing={1}>
         {systemsByTag.map((k,i)=>{
            return <RenderTagSystemsAccordion key={i} tagSolarSystems={k}/>
         })}
      </Stack>
      </div> :
      <div>
        <CircularProgress/>
      </div>
    }

  </div>
}
