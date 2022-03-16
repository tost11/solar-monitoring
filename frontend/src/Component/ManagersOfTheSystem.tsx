import React, {useState} from "react"
import {getManagers, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import ManagerComponent from "./ManagerComponent";
import {Button} from "@mui/material";
import SearchUser from "./SearchUser";
import {GenericDataDTO} from "../api/UserAPIFunctions";

interface ManagersOfTheSystemProps{
  systemId:number,
  initManagers: ManagerDTO[]
}

export default function ManagersOfTheSystem({systemId,initManagers}:ManagersOfTheSystemProps){
  const [listOfManagers,setListOfManagers] = useState<ManagerDTO[]>(initManagers);

  //manager for search field
  const [userToManager,setUserToManager]=useState<GenericDataDTO|null>(null)

  return<div>
    <SearchUser setUser={setUserToManager}/>

    <Button disabled={!userToManager} variant="outlined" onClick={()=>{
      // @ts-ignore
      setManageUser({id:userToManager.id,systemId:systemId,role:"VIEW"}).then((r)=> {setListOfManagers(r.managers)})
    }}>AddUser As Manager</Button>
    {listOfManagers&&
    listOfManagers.map((m, i) =>
      <ManagerComponent key={i} manager={m} systemId={systemId} setListOfManagers={setListOfManagers}/>)
    }


  </div>
}



