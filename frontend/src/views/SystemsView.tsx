import React, {useContext, useEffect, useState} from "react";
import {
  searchSystems,
  SolarSystemListDTO,
  SolarSystemSearchParams,
  SolarSystemType
} from "../api/SolarSystemAPI";
import SystemAccordion from "../Component/Accordions/SystemAccordion";
import {Button, Switch, TextField} from "@mui/material";
import {useNavigate} from "react-router-dom";
import {UserContext} from "../context/UserContext";
import TagModal from "../Component/TagModal";
import {TagDTO} from "../api/UserAPIFunctions";
import TagView from "../Component/TagView";
import SolarSystemTypeSelect from "../Component/SolarSystemTypeSelect";

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

interface RenderSearchParamsProps {
  onFilterChange:(searchParams:SolarSystemSearchParams)=>void
}

function RenderSearchParams({onFilterChange}:RenderSearchParamsProps){

  const login = useContext(UserContext)
  const [open,setOpen] = useState(false)
  const [tags,setTags] = useState<TagDTO[]>([])
  const [name, setName] = useState<string|undefined>(undefined)
  const [type, setType] = useState<SolarSystemType|undefined>(undefined)
  const [isPublic, setIsPublic] = useState(false)
  const [nameTimeout,setNameTimout] = useState<number|undefined>(undefined)

  const doUpdate=()=>{
    onFilterChange({
      public: login ? isPublic : true,
      name: name,
      type: type,
      tags: tags.length === 0 ? undefined : tags.map(t=>t.id)
    })
  }

  useEffect(() => {
    doUpdate()
  }, [tags,type,isPublic]);

  const addTagToTags = (tag:TagDTO)=>{
    let newData = [...tags]
    newData.push(tag);
    setTags(newData);
  }

  useEffect(() => {
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

  return <div style={{backgroundColor:"floralwhite",marginTop:"10px"}}>
    <TagModal currentTags={[]} addTag={addTagToTags} onClose={()=>setOpen(false)} open={open}/>
    <div className="defaultFlex" style={{padding: "10px"}}>
      <div className="flexColumn">
        <less style={{fontWeight: "bold"}}>Search for Name</less>
        <TextField helperText={name !== undefined && name.length < 3 ? "search term to short":undefined} error={name !== undefined && name.length < 3} value={name} onChange={(ev)=>setName(ev.target.value.length === 0 ? undefined : ev.target.value)} variant="outlined"/></div>
      <div className="flexColumn">
        <less style={{fontWeight: "bold"}}>Search for Type</less>
        <SolarSystemTypeSelect fontSize="large" setSelected={setType} selected={type} renderClear={true}/>
      </div>
      {login && <div className="flexColumn">
        <less style={{fontWeight: "bold"}}>Is Public</less>
        <Switch value={isPublic} onClick={()=>setIsPublic(!isPublic)} defaultChecked/></div>}
      <div className="flexColumn">
        <less style={{fontWeight: "bold"}}>Tags <Button onClick={()=>setOpen(true)}>Add Tags</Button></less>
        <TagView tags={tags} onDelete={deleteTagFromSystem} showDelete={true}/>
      </div>
    </div>
  </div>
}

export default function SystemsView() {

  const [data, setData] = useState<SolarSystemListDTO[]>([])
  const [compareMap, setCompareMap] = useState(new Map<string,string>)

  const navigate = useNavigate();

  const reloadSystems = (searchParams:SolarSystemSearchParams) => {
    searchSystems(searchParams).then((res) => {
      setData(res)
    })
  }

  useEffect(()=>reloadSystems({}),
    [])
  return <div>
    <RenderSearchParams onFilterChange={reloadSystems}/>
    {data.length > 0 ? <>
      {data.map((e,i)=>
        <div style={{marginTop: "7px"}}>
          <SystemAccordion isInCompareList={compareMap.has(e.id)} key={i} system={e} reloadSystems={reloadSystems} setInCompareList={sel=>{
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

