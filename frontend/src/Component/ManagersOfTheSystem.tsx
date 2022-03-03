import React, {useEffect, useState} from "react"
import {getManagers, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import ManagerComponent from "./ManagerComponent";

interface ManagersOfTheSystemProps{
  systemId:number
}



export default function ManagersOfTheSystem({systemId}:ManagersOfTheSystemProps){
  const [listOfManagers,setListOfManagers] = useState<ManagerDTO[]>([]);
  useEffect(() => {
    getManagers(systemId).then(r =>{
      setListOfManagers(r)
    })
  },[])

  return<div>
    {listOfManagers.length>0&&
      listOfManagers.map((m,i)=>
      <ManagerComponent key={i} manager={m} systemId={systemId}/>)

    }
  </div>
}



