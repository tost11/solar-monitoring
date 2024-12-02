import React, {useEffect, useState} from "react";
import {CircularProgress, TextField, Checkbox, FormControlLabel} from "@mui/material";
import {apiCreateTag, apiGetAvailableTags, AdminTagDTO, apiGetTags} from "../api/UserAPIFunctions";
import Button from "@mui/material/Button";

interface EditTag{
  AdminTagDTO: AdminTagDTO,
  edit: boolean
}

function RenderTag({tag,onEdit}){
    return <div style={{
      display: "flex",
      display: "flex",
      flexWrap: "wrap",
      gap: "10px",
      backgroundColor: "white",
      margin: "auto",
      borderRadius: "10px",
      padding: "10px"
    }}>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>Name: {tag.name}</div>
      <div style={{marginTop:"auto",marginBottom:"auto",color:tag.color}}>Color: {tag.color}</div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <FormControlLabel
          label={<div>Locked</div>}
          control={<Checkbox disabled={true} checked={tag.locked}/>}/>
      </div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <FormControlLabel
          label={<div>Show on Start Page</div>}
          control={<Checkbox disabled={true} checked={tag.showOnStartPage}/>}/>
      </div>
      <Button onClick={onEdit} variant="contained">Edit</Button>
    </div>
}

function RenderEditTag({tag, onSave, onAbort}) {

  const [name, setName] = useState(tag.name)
  const [color, setColor] = useState(tag.color)
  const [locked, setLocked] = useState<boolean>(tag.locked)
  const [showOnStartPage, setShowOnStartPage] = useState<boolean>(tag.locked)

  {/* TODO some more validation*/}
  return <div style={{flexWrap: "wrap",display:"flex",gap:"10px",backgroundColor: "white", margin: "auto", borderRadius: "10px",padding:"10px"}}>
    <div><TextField onChange={ev => setName(ev.target.value)} type="text" placeholder="new awesome tag"
                     label="Tag Name" value={name}/></div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}><input type="color" value={color} onChange={(ev)=>setColor(ev.target.value)} /></div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}>
      <FormControlLabel
        label={<div>Locked</div>}
        control={<Checkbox onChange={() => setShowOnStartPage(!showOnStartPage)} checked={showOnStartPage}/>}/>
    </div>
    <div style={{marginTop:"auto",marginBottom:"auto"}}>
      <FormControlLabel
        label={<div>Show on Start Page</div>}
        control={<Checkbox onChange={() => setLocked(!locked)} checked={locked}/>}/>
    </div>
    <Button onClick={()=>onSave({id:tag.id,name,color,locked,showOnStartPage})} variant="contained">{tag.id ? "Edit" : "Create"}</Button>
    {tag.id != undefined &&
      <Button onClick={onAbort} variant="contained">Abort</Button>
    }
  </div>
}

export default function TagsView() {
  const [tags, setTags] = useState<EditTag[]>();
  const [newTag, setNewTag] = useState({name:"",id:undefined,color:"#00FF00",locked:false,showOnStartPage:false})

  const addTag = (tag:AdminTagDTO)=>{
    apiCreateTag(tag).then(res=>{
      addTagToList(res);
      setNewTag({name:"",id:undefined,color:"#00FF00",locked:false,showOnStartPage:false})
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
    <h2>List of all Tags</h2>

    {tags ?
      <>
        <h3>Create new tag</h3>
        <RenderEditTag onSave={addTag} tag={newTag}/>

        <h3>All tags</h3>
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
      <div><CircularProgress/> Loading Tags info</div>
    }
  </div>)
}
