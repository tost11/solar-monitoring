import React, {useEffect, useState} from "react";
import CircularProgress from "@mui/material/CircularProgress";
import TextField from "@mui/material/TextField";
import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
import Button from "@mui/material/Button";
import {apiCreateTag, AdminTagDTO, apiGetTags} from "../api/UserAPIFunctions";
import {useTranslation} from "react-i18next";
import {useNavigate} from "react-router-dom";

interface EditTag{
  AdminTagDTO: AdminTagDTO,
  edit: boolean
}

function RenderTag({tag,onEdit}){

    const { t } = useTranslation();
    const navigate = useNavigate();

    return <div className="defaultFlex" style={{
      backgroundColor: "white",
      margin: "auto",
      borderRadius: "10px",
      padding: "10px"
    }}>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>Name: {tag.name}</div>
      <div style={{marginTop:"auto",marginBottom:"auto",color:tag.color}}>Color: {tag.color}</div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <FormControlLabel
          label={<div>{t("common.locked")}</div>}
          control={<Checkbox disabled checked={tag.locked}/>}/>
      </div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <FormControlLabel
          label={<div>{t("views.tags.start_page")}</div>}
          control={<Checkbox disabled checked={tag.showOnStartPage}/>}/>
      </div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <FormControlLabel
          label={<div>{t("views.tags.start_page_aggregation")}</div>}
          control={<Checkbox disabled checked={tag.showStartPageAggregation}/>}/>
      </div>
      <Button onClick={() => navigate(`/tag/${tag.id}`)} variant="outlined">{t("views.tags.view_tag")}</Button>
      <Button onClick={onEdit} variant="contained">{t("common.edit")}</Button>
    </div>
}

function RenderEditTag({tag, onSave, onAbort}) {

  const { t } = useTranslation();

  const [name, setName] = useState(tag.name)
  const [color, setColor] = useState(tag.color)
  const [locked, setLocked] = useState<boolean>(tag.locked)
  const [showOnStartPage, setShowOnStartPage] = useState<boolean>(tag.showOnStartPage)
  const [showStartPageAggregation, setShowStartPageAggregation] = useState<boolean>(tag.showStartPageAggregation ?? false)

  {/* TODO some more validation*/}
  return <div className="defaultFlex" style={{backgroundColor: "white", margin: "auto", borderRadius: "10px",padding:"10px"}}>
    <div><TextField onChange={ev => setName(ev.target.value)} type="text" placeholder={t("views.tags.new_tag_placeholder")}
                     label={t("views.tags.name")} value={name}/></div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}><input type="color" value={color} onChange={(ev)=>setColor(ev.target.value)} /></div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}>
      <FormControlLabel
        label={<div>{t("common.locked")}</div>}
        control={<Checkbox onChange={() => setLocked(!locked)} checked={locked}/>}/>
    </div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}>
      <FormControlLabel
        label={<div>{t("views.tags.start_page")}</div>}
        control={<Checkbox onChange={() => setShowOnStartPage(!showOnStartPage)} checked={showOnStartPage}/>}/>
    </div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}>
      <FormControlLabel
        label={<div>{t("views.tags.start_page_aggregation")}</div>}
        control={<Checkbox onChange={() => setShowStartPageAggregation(!showStartPageAggregation)} checked={showStartPageAggregation}/>}/>
    </div>
    <Button onClick={()=>onSave({id:tag.id,name,color,locked,showOnStartPage,showStartPageAggregation})} variant="contained">{tag.id ? t("common.edit") : t("common.create")}</Button>
    {tag.id != undefined &&
      <Button onClick={onAbort} variant="contained">{t("common.abort")}</Button>
    }
  </div>
}

export default function TagsView() {

  const { t } = useTranslation();

  const [tags, setTags] = useState<EditTag[]>();
  const [newTag, setNewTag] = useState({name:"",id:undefined,color:"#00FF00",locked:false,showOnStartPage:false,showStartPageAggregation:false})

  const addTag = (tag:AdminTagDTO)=>{
    apiCreateTag(tag).then(res=>{
      addTagToList(res);
      setNewTag({name:"",id:undefined,color:"#00FF00",locked:false,showOnStartPage:false,showStartPageAggregation:false})
    });
  }

  const addTagToList = (tag:AdminTagDTO)=>{
    var newList = [...tags];
    for (let elem of newList) {
      if(elem.AdminTagDTO.id === tag.id){
        elem.AdminTagDTO = tag;
        elem.edit = false;
        return;
      }
    }
    newList.push({AdminTagDTO:tag,edit:false});
    setTags(newList);
  }

  const setEditTag = (id,value)=>{
    var newTags = [...tags];
    for (let tag of newTags) {
      if(tag.AdminTagDTO.id === id){
        tag.edit = value;
        setTags(newTags);
        return
      }
    }
  }

  useEffect(() => {
    apiGetTags().then(res=>{
      setTags(res.map((v) => {return {AdminTagDTO: v,edit:false,edited: true,showOnStartPage:false}}));
    });
  }, []);

  return (<div>
    <h2>{t("views.tags.list")}</h2>

    {tags ?
      <>
        <h3>{t("views.tags.add")}</h3>
        <RenderEditTag onSave={addTag} tag={newTag}/>

        <h3>{t("views.tags.available")}</h3>
        <div>
          {tags.map((editTag, i) => {
            return <div style={{margin: "15px"}} key={i}>
              {editTag.edit ?
                <RenderEditTag onAbort={()=>setEditTag(editTag.AdminTagDTO.id,false)} onSave={addTag} tag={editTag.AdminTagDTO}/> :
                <RenderTag tag={editTag.AdminTagDTO} onEdit={() => setEditTag(editTag.AdminTagDTO.id, true)}/>
              }
            </div>
          })
          }
        </div>
      </> :
      <div><CircularProgress/> {t("views.tags.loading")}</div>
    }
  </div>)
}
