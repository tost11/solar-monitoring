import React, {useEffect, useState} from "react";
import {createNewToken, getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import CreateNewSystemComponent from "./CreateNewSystemComponent";
import {Button} from "@mui/material";

export default function EditSystemComponent() {
  const initialState = {
    name:"",
    buildingDate:new Date(),
    creationDate:new Date(),
    type:"",
    id:0,
    isBatteryPercentage:false,
    inverterVoltage:0,
    batteryVoltage:0,
    maxSolarVoltage:0,
    managers:[]
  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)
  const [change, setChange] = useState(false)

  const params = useParams()

  useEffect(() => {
    if (!isNaN(Number(params.id))) {
      getSystem("" + params.id).then((res) => {
        setData(res)
        console.log(res)
      }).then(() =>
        setIsLoading(true))
    }
  }, [])

  return <div>

    {isLoading&&
    <CreateNewSystemComponent data={data}/>&&
      <Button onClick={()=>{
      createNewToken(data.id).then((response)=>{
        alert(response.token)
      })
    }}>Create a new Token
      </Button>
    }

  </div>
}
