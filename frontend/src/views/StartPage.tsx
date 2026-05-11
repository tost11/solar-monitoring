import React, {useEffect, useState} from "react";
import {
  getSystemsByTag, SolarSystemListDTO,
  TagSolarSystemDTO
} from "../api/SolarSystemAPI";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary, Box,
  CircularProgress, Divider,
  Stack, Tabs
} from "@mui/material";
import {useLocation, useNavigate} from "react-router-dom";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import Button from "@mui/material/Button";
import {formatDefaultValueWithUnit} from "../Component/utils/GraphUtils";
import Tab from '@mui/material/Tab';
import {TabContext, TabList, TabPanel} from "@mui/lab";
import {useTranslation} from "react-i18next";

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

  const { t } = useTranslation()

  const numOnline = getOnlineSystems(tagSolarSystemDTO.systems);
  const dif = numOnline / tagSolarSystemDTO.systems.length;
  const totalInputWatt = getTotalProduction(tagSolarSystemDTO.systems);
  const totalProducedWH = getTotalProducedWH(tagSolarSystemDTO.systems);

  return <Accordion key={key} defaultExpanded={true}>
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
            <Button onClick={()=>navigate("/dd/"+k.id)}>{t("views.start_page.detail_view")}</Button>
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

  const { t } = useTranslation()

  const [systemsByTag,setSystemsByTag] = useState<TagSolarSystemDTO[]>()

  useEffect(()=>{
      getSystemsByTag().then(res=>{
        setSystemsByTag(res)
    }
  )}
  ,[])

  const [tabValue, setTabValue] = React.useState("1");
  const handleChange = (event: React.SyntheticEvent, newValue: string) => {
    setTabValue(newValue);
  };

  return<div style={{display:"flex",justifyContent:"center",flexDirection: "column"}}>
    <h1>{t("views.start_page.heading_1")}</h1>
    <div>
      {t("views.start_page.text_1")}
    </div>

    <h2>{t("views.start_page.heading_2")}</h2>
    <Divider />
    <Box sx={{ width: '100%', typography: 'body1' }}>
      <TabContext value={tabValue}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} variant="scrollable" scrollButtons="auto" onChange={handleChange}>
            <Tab label={t("views.start_page.motivation_heading")} value="1" />
            <Tab label={t("views.start_page.registration_heading")} value="2" />
            <Tab label={t("views.start_page.acces_heading")} value="3" />
            <Tab label={t("views.start_page.open_source_heading")} value="4" />
            <Tab label={t("views.start_page.clients_heading")} value="5" />
          </Tabs>
        </Box>
        <TabPanel value="1">
          {t("views.start_page.motivation_1")}
          <br/><br/>
          {t("views.start_page.motivation_2")}
        </TabPanel>
        <TabPanel value="2">
          {t("views.start_page.registration_1")}
        </TabPanel>
        <TabPanel value="3">
          {t("views.start_page.acces_1")}
          <br/><br/>
          {t("views.start_page.acces_2")}
        </TabPanel>
        <TabPanel value="4">
          {t("views.start_page.open_source_1")}
          <br/><br/>
          {t("views.start_page.open_source_2")}
          <br/>
          <a href="https://github.com/tost11/solar-monitoring">https://github.com/tost11/solar-monitoring</a>
        </TabPanel>
        <TabPanel value="5">
          {t("views.start_page.clients_1")}
          <br/>
          <a href="https://github.com/tost11/solar-monitoring/tree/develop/client">https://github.com/tost11/solar-monitoring/tree/develop/client</a>
          <br/><br/>
          {t("views.start_page.clients_2")}
          <br/><br/>
          {t("views.start_page.clients_3")}
          <br/>
          <a href="https://github.com/tost11/OpenDTU-Push-Rest-API-and-Deye-Sun/tree/feature/push-rest-api">https://github.com/tost11/OpenDTU-Push-Rest-API-and-Deye-Sun/tree/feature/push-rest-api</a>
        </TabPanel>
      </TabContext>
    </Box>
    <Divider />

    <h2>{t("views.start_page.heading_3")}</h2>
    <div>{t("views.start_page.text_3")}</div>

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
