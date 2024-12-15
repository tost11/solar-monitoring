import {TagDTO} from "../api/UserAPIFunctions";
import DeleteForeverIcon from "@mui/icons-material/DeleteForever";
import React from "react";

interface TagViewProps {
  tags: TagDTO[],
  onDelete: (tag:TagDTO)=>void,
  showDelete: boolean
}

export default function TagView({tags,onDelete,showDelete}: TagViewProps) {
  return <div style={{flexWrap: "wrap", display: "flex", padding: "10px", gap: "10px"}}>
    {tags.map((tag, i) => {
        return <div key={i} style={{
          display: "flex",
          flexWrap: "wrap",
          backgroundColor: tag.color,
          padding: "10px",
          paddingLeft: "15px",
          paddingRight: "15px",
          borderRadius: "20px"
        }}>
          <div style={{margin: "auto"}}>{tag.name}</div>
          {showDelete && <DeleteForeverIcon style={{padding: "2px", cursor: "pointer"}} onClick={() => onDelete(tag)}
                             fontSize="small"/>}
        </div>
      }
    )}
  </div>
}
