import {Button} from "@mui/material";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import React from "react";

interface TotalFilterListProps{
  filters: string[],
  onDelete: (filter:string)=>void,
  loading: boolean
}

export default function TotalFilterList({filters, onDelete, loading}:TotalFilterListProps) {
  return <div>
    {filters.length > 0 && <div style={{display: "flex",flexDirection: "column",flexWrap: "wrap"}}>
      {filters.map((filter,i)=><div key={i}>
        <div style={{marginLeft: "10px",marginRight: "10px", display: "flex", alignItems: "center"}}>
          <span>{filter}</span>
          <IconButton disabled={loading} onClick={()=>onDelete(filter)}><DeleteIcon/></IconButton>
        </div>
      </div>)}
    </div>
    }
  </div>
}
