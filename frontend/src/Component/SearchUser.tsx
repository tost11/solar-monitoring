import {TextField} from "@mui/material";
import Autocomplete from "@mui/material/Autocomplete";
import Stack from "@mui/material/Stack";
import React, {useState} from "react";
import {findUsers, GenericDataDTO} from "../api/UserAPIFunctions";

interface SearchUserProps {
  setUser: (v: GenericDataDTO|null) => void
}

export default function SearchUser({setUser}: SearchUserProps) {
  const [userList, setUserList] = useState<GenericDataDTO[]>([])
  const [selected, setSelected] = useState<GenericDataDTO | null>(null);
  const [timer,setTimer] = useState<NodeJS.Timeout|null>(null);

  const setNewTimer = (v:()=>void)=>{
    if(timer){
      clearTimeout(timer);
    }
    setTimer(setTimeout(v,400))
  }

  const searchInputChanged = (event:any)=>{
    let nameToFind = event.target.value;
    if(nameToFind.length < 3){
      return;
    }
    setNewTimer(()=>{
      console.log(nameToFind)
      findUsers(nameToFind).then((r) => {
        setUserList(r);
      })
    })
  }

  const userSelected = (event:any,newValue:any)=>{

    console.log(event.target.textContent)

    if(event.target.textContent === ""){
      setUserList([])
      setUser(null)
      return
    }

    let obj = userList.find((v)=>v.name === event.target.textContent);
    if(!obj){
      return;
    }

    setSelected(obj)
    setUser(obj)
  }
///TODO refactor that userAre can Add if you press enter.
  return (
    <Stack spacing={2} sx={{width: 300}}>
      <Autocomplete
        value={selected}
        options={userList}
        onChange={userSelected}
        getOptionLabel={(option)=>option.name}
        selectOnFocus
        clearOnBlur
        handleHomeEndKeys
        sx={{width: 300}}
        freeSolo
        renderInput={(params) => <TextField {...params} onChange={searchInputChanged}/>}/>
    </Stack>
  );
}
