import React, {useState} from "react"
import {getManagers, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import ManagerComponent from "./ManagerComponent";
import {Button} from "@mui/material";
import SearchUser from "./SearchUser";
import {UserDTO} from "../api/UserAPIFunctions";

interface ManagersOfTheSystemProps{
  systemId:number,
  initManagers: ManagerDTO[]
}



export default function ManagersOfTheSystem({systemId,initManagers}:ManagersOfTheSystemProps){
  const [listOfManagers,setListOfManagers] = useState<ManagerDTO[]>(initManagers);

  //manager for search field
  const [userToManager,setUserToManager]=useState<UserDTO>({id:0,name:"",numAllowedSystems:0,admin:false,deleted:false})


  return<div>
    <SearchUser setUser={setUserToManager}/>

    <Button variant="outlined" onClick={()=>{
      setManageUser(userToManager.id,systemId,"VIEW").then((r)=>{setListOfManagers(r.managers)})

    }}>AddUser As Manager</Button>

    {listOfManagers&&
    listOfManagers.map((m, i) =>
      <ManagerComponent key={i} manager={m} systemId={systemId}/>)
    }


  </div>
}



