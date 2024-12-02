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
    if(open) {
      apiGetAvailableTags().then(res => {
        setTags(res);
      })
    }
  },[open])

  return <Modal
    open={open}
    onClose={closeModal}
    aria-labelledby="modal-modal-title"
    aria-describedby="modal-modal-description"
  >

    <Box className={"Modal"} >
      {tags ?
        <div style={{flexWrap:"wrap", display:"flex",padding:"10px",gap:"10px"}}>
          {tags.map((tag,i)=>{
            let inCurrentTags = currentTags.find((curTag)=>curTag.id === tag.id);
            return <div key={i} style={{
              display: "flex",
              flexWrap: "wrap",
              backgroundColor: tag.color,
              padding: "10px",
              paddingLeft: "15px",
              paddingRight: "15px",
              borderRadius: "20px",
              cursor: inCurrentTags ? "auto" :"pointer"
            }} onClick={() => {
              if (!inCurrentTags) {
                addTag(tag)
              }
            }}>{tag.name}{inCurrentTags && " (used)"}</div>
          })}
        </div> :
        <div><CircularProgress/> Loading Tags...</div>
      }
    </Box>
  </Modal>
}
