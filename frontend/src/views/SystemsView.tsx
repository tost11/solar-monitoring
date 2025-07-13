import React, {useContext, useEffect, useRef, useState} from "react";
import {
  findTagsById,
  searchSystems,
  SolarSystemListDTO,
  SolarSystemSearchParams,
  SolarSystemType
} from "../api/SolarSystemAPI";
import SystemAccordion from "../Component/Accordions/SystemAccordion";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  CircularProgress,
  Link,
  Switch,
  TextField
} from "@mui/material";
import {useNavigate, useSearchParams} from "react-router-dom";
import {UserContext} from "../context/UserContext";
import TagModal from "../Component/TagModal";
import {TagDTO} from "../api/UserAPIFunctions";
import TagView from "../Component/TagView";
import SolarSystemTypeSelect from "../Component/SolarSystemTypeSelect";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {useTranslation} from "react-i18next";

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

export interface RenderSearchParamsPropsInitData{
  public?: boolean,
  name?: string,
  tags: TagDTO[],
  type?: SolarSystemType
}

interface RenderSearchParamsProps {
  onFilterChange:(searchParams:SolarSystemSearchParams)=>void
  initData: RenderSearchParamsPropsInitData
}

function RenderSearchParams({onFilterChange,initData}:RenderSearchParamsProps){

  const { t } = useTranslation();

  const login = useContext(UserContext)
  const [open,setOpen] = useState(false)
  const [tags,setTags] = useState<TagDTO[]>(initData.tags)
  const [name, setName] = useState(initData.name)
  const [type, setType] = useState(SolarSystemType[initData.type])
  const [isPublic, setIsPublic] = useState(initData.public)
  const [nameTimeout,setNameTimout] = useState<number|undefined>(undefined)
  const effect1 = useRef(false);
  const effect2 = useRef(false);

  const navigate = useNavigate()

  const doUpdate=()=>{

    let searches = [];
    if(tags.length > 0){
      searches.push("tag=" + tags.map(t=>t.id).reduce((pre, next)=>pre + ',' + next))
    }
    if(name && name.length >= 3){
      searches.push("name=" + name)
    }
    if(type){
      searches.push("type=" + type)
    }
    if(isPublic){
      searches.push("public=" + (isPublic?"1":"0"))
    }

    navigate({
      pathname: location.pathname,
      search: searches.length >0 ? "?"+searches.reduce((pre, next)=>pre + '&' + next): ""
    }, {replace: true})

    onFilterChange({
      public: login ? isPublic : true,
      name: name,
      type: type,
      tags: tags.length === 0 ? undefined : tags.map(t=>t.id)
    })
  }

  useEffect(() => {
    if(!effect1.current){
      effect1.current = true
      return;
    }
    doUpdate()
  }, [tags,type,isPublic]);

  const addTagToTags = (tag:TagDTO)=>{
    let newData = [...tags]
    newData.push(tag);
    setTags(newData);
  }

  useEffect(() => {
    if(!effect2.current){
      effect2.current = true
      return;
    }
    if(nameTimeout !== undefined){
      clearTimeout(nameTimeout)
    }
    if(name == undefined || name.length >= 3){
      setNameTimout(setTimeout(()=>doUpdate(),500))
    }
  }, [name]);


  const deleteTagFromSystem = (tag:TagDTO)=>{
      let newTags = []
      for (let t of tags) {
        if(t.id !== tag.id){
          newTags.push(t);
        }
      }
      setTags(newTags);
  }

  const getNumActiveFilters = ()=>{
    let num = 0;
    if(isPublic === true){
      num++;
    }
    if(name && name.length > 0){
      num++;
    }
    if(type){
      num++;
    }
    if(tags && tags.length > 0){
      num++;
    }
    return num;
  }

  return <>
    <TagModal currentTags={[]} addTag={addTagToTags} onClose={()=>setOpen(false)} open={open}/>
    <Accordion style={{backgroundColor:"floralwhite",marginTop:"10px"}}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon/>}
        aria-controls="panel1a-content"
        id="panel1a-header"
      >
        <div style={{padding: "20px", paddingBottom: "0px", paddingTop: "0px", fontSize: "25px"}}>{t("views.systems_list.search_filters")} ({t("common.active")} {""+getNumActiveFilters()})</div>
      </AccordionSummary>
      <AccordionDetails>
        <div className="defaultFlex" style={{padding: "10px", paddingTop: "0px"}}>
          <div className="searchParamField">
            <span className="searchParamFieldFirst">{t("common.name")}</span>
            <div className="searchParamFieldSecond"><TextField helperText={name?.length < 3 ? "search term to short":undefined} error={name?.length < 3} value={name} onChange={(ev)=>setName(ev.target.value.length > 0 ? ev.target.value : undefined)} variant="outlined"/></div>
          </div>
          <div className="searchParamField">
            <span className="searchParamFieldFirst">{t("views.systems_list.system_type")}</span>
            <div className="searchParamFieldSecond"><SolarSystemTypeSelect preferredWidth="185px" fontSize="large" setSelected={setType} selected={type} renderClear={true}/></div>
          </div>
          {login && <div className="searchParamField">
            <span className="searchParamFieldFirst">{t("common.public")}</span>
            <div className="searchParamFieldSecond"><Switch checked={isPublic} onClick={()=>setIsPublic(!isPublic)} defaultChecked/></div>
          </div>}
          <div className="searchParamField">
            <span className="searchParamFieldFirst">{t("common.tags")}<Link style={{marginLeft:"5px",cursor:"pointer"}} underline="none" onClick={()=>setOpen(true)}>{t("views.systems_list.add_tag")}</Link></span>
            <div className="searchParamFieldSecond"><TagView tags={tags} onDelete={deleteTagFromSystem} showDelete={true}/></div>
          </div>
        </div>
      </AccordionDetails>
    </Accordion>
  </>
}

export default function SystemsView() {

  const { t } = useTranslation();

  const [data, setData] = useState<SolarSystemListDTO[]|undefined>(undefined)
  const [compareMap, setCompareMap] = useState(new Map<string,string>())
  const [initData,setInitData] = useState<RenderSearchParamsPropsInitData|undefined>(undefined);

  const [searchParams] = useSearchParams()

  const navigate = useNavigate();

  const reloadSystems = (searchParams:SolarSystemSearchParams) => {
    setCompareMap(new Map<string,string>());
    searchSystems(searchParams).then((res) => {
      setData(res)
    })
  }

  useEffect(()=> {
    let initD = {
      public: searchParams.get("public") === "1",
      name: searchParams.get("name"),
      type: SolarSystemType[searchParams.get("type")],
      tags: []
    } as RenderSearchParamsPropsInitData
    const ids = searchParams.getAll("tag")
    if (ids && ids.length > 0) {
      findTagsById(ids).then(res=>{
        initD.tags = res;
        setInitData(initD)
      })
    } else {
      setInitData(initD)
    }
  }, [])

  useEffect(() => {
    if(initData){
      reloadSystems({
        public: initData.public,
        name: initData.name,
        type: initData.type,
        tags: initData.tags.map(t=>t.id)
      })
    }
  }, [initData]);

  return <div>
    {initData && data ? <>
      <RenderSearchParams initData={initData} onFilterChange={reloadSystems}/>
      {data.length > 0 ? <>
        {data.map((e,i)=>
          <div key={i} style={{marginTop: "7px"}}>
            <SystemAccordion isInCompareList={compareMap.has(e.id)} system={e} reloadSystems={reloadSystems} setInCompareList={sel=>{
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
    </>:
    <>
      Loading Data <CircularProgress/>
    </>}
  </div>
}

