import React, {useEffect, useState} from "react";
import {CircularProgress, TextField, Checkbox, FormControlLabel} from "@mui/material";
import {apiCreateTag, apiGetAvailableTags, TagDTO} from "../api/UserAPIFunctions";
import Button from "@mui/material/Button";

interface EditTag{
  tagDTO: TagDTO,
  edit: boolean
}

function RenderTag({tag,onEdit}){
    return <div style={{
      display: "flex",
      gap: "10px",
      backgroundColor: "white",
      margin: "auto",
      borderRadius: "10px",
      padding: "10px"
    }}>
      <div>Name: {tag.name}</div>
      <div style={{color:tag.color}}>Color: {tag.color}</div>
      <div>
        <FormControlLabel
          label={<div>Locked</div>}
          control={<Checkbox disabled={true} checked={tag.locked}/>}/>
      </div>
      <Button onClick={onEdit} variant="contained">Edit</Button>
    </div>
}

function RenderEditTag({tag, onSave, onAbort}) {

  const [name, setName] = useState(tag.name)
  const [color, setColor] = useState(tag.color)
  const [locked, setLocked] = useState<boolean>(tag.locked)

  {/* TODO some more validation*/}
  return <div style={{display:"flex",gap:"10px",backgroundColor: "white", margin: "auto", borderRadius: "10px",padding:"10px"}}>
    <div><TextField onChange={ev => setName(ev.target.value)} type="text" placeholder="new awesome tag"
                     label="Tag Name" value={name}/></div>
    <div><input type="color" value={color} onChange={(ev)=>setColor(ev.target.value)} /></div>
    <div>
      <FormControlLabel
        label={<div>Locked</div>}
        control={<Checkbox onChange={() => setLocked(!locked)} checked={locked}/>}/>
    </div>
    <Button onClick={()=>onSave({id:tag.id,name,color,locked})} variant="contained">{tag.id ? "Edit" : "Create"}</Button>
    {tag.id != undefined &&
      <Button onClick={onAbort} variant="contained">Abort</Button>
    }
  </div>
}

export default function TagsView() {
  const [tags, setTags] = useState<EditTag[]>();
  const [newTag, setNewTag] = useState({name:"",id:undefined,color:"ffffff",locked:false})

  const addTag = (tag:TagDTO)=>{
    apiCreateTag(tag).then(res=>{
      addTagToList(res);
      setNewTag({name:"",id:undefined,color:"ffffff",locked:false})
    });
  }

  const addTagToList = (tag:TagDTO)=>{
    var newList = [...tags];
    for (let elem of newList) {
      if(elem.tagDTO.id === tag.id){
        elem.tagDTO = tag;
        elem.edit = false;
        return;
      }
    }
    newList.push({tagDTO:tag,edit:false});
    setTags(newList);
  }

  const setEditTag = (id,value)=>{
    var newTags = [...tags];
    for (let tag of newTags) {
      if(tag.tagDTO.id === id){
        tag.edit = value;
        setTags(newTags);
        return
      }
    }
  }

  useEffect(() => {
    apiGetAvailableTags().then(res=>{
      setTags(res.map((v) => {return {tagDTO: v,edit:false,edited: true}}));
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
                <RenderEditTag onAbort={()=>setEditTag(editTag.tagDTO.id,false)} onSave={addTag} tag={editTag.tagDTO}/> :
                <RenderTag tag={editTag.tagDTO} onEdit={() => setEditTag(editTag.tagDTO.id, true)}/>
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
