import React, {useEffect, useState} from "react"
import {getManagers, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import ManagerComponent from "./ManagerComponent";
import {Button} from "@mui/material";
import SearchUser from "./SearchUser";
import {UserDTO} from "../api/UserAPIFunctions";

interface ManagersOfTheSystemProps{
  systemId:number
}



export default function ManagersOfTheSystem({systemId}:ManagersOfTheSystemProps){
  const [listOfManagers,setListOfManagers] = useState<ManagerDTO[]>([]);
  const [listOfResponseManagers,setListOfResponseManagers] = useState<ManagerDTO[]>([]);

  const [userToManager,setUserToManager]=useState<UserDTO>({id:0,name:"",numAllowedSystems:0,admin:false})
  useEffect(() => {
    getManagers(systemId).then(r =>{
      setListOfManagers(r)
    })
  },[listOfResponseManagers])

  return<div>
    <SearchUser setUser={setUserToManager}/>

    <Button variant="outlined" onClick={()=>{
      setManageUser(userToManager.id,systemId,"VIEW").then((r)=>{
        setListOfResponseManagers(r)
      })
    }}>AddUser As Manager</Button>
    {listOfManagers&&
    listOfManagers.map((m, i) =>
      <ManagerComponent key={i} manager={m} systemId={systemId}/>)
    }


  </div>
}



