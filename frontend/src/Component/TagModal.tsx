import React, {useEffect, useState} from "react";
import {Box, CircularProgress, Modal} from '@mui/material';
import {apiGetAvailableTags, TagDTO} from "../api/UserAPIFunctions";


interface TagModalProps {
  currentTags: TagDTO[]
  addTag: (tag: TagDTO) => void
  onClose:()=>void
  open:boolean
}

export default function TagModal({addTag,onClose,open,currentTags}: TagModalProps) {
  const [tags, setTags] = useState<TagDTO[]>()

  const closeModal = () => {
    setTags(undefined)
    onClose()
  }

  useEffect(()=> {
      apiGetAvailableTags().then(res => {
        setTags(res);
      })
  },[])

  return <Modal
    open={open}
    onClose={closeModal}
    aria-labelledby="modal-modal-title"
    aria-describedby="modal-modal-description"
  >

    <Box className={"Modal"} >
      {tags ?
        <div style={{display:"flex"}}>
          {tags.map((tag,i)=>{
            let inCurrentTags = currentTags.find((curTag)=>curTag.id === tag.id);
            return <div style={{margin:"15px",borderRadius:"5px"}} key={i} onClick={()=>{
              if(!inCurrentTags) {
                addTag(tag)
              }
            }} style={{backgroundColor:tag.color}}>{tag.name}{inCurrentTags && "(used)"}</div>
          })}
        </div>:
        <div><CircularProgress/> Loading Tags...</div>
      }
    </Box>

  </Modal>
}
