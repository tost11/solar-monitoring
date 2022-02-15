import React, {useEffect, useState} from "react";
import {getSystem, RegisterSolarSystemDTO, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import {Button, CircularProgress, Input, TextField} from "@mui/material";
import CreateNewSystemComponent from "./CreateNewSystemComponent";

export default function EditSystemComponent() {
  const initialState = {
    name:"",
    buildingDate:0,
    creationDate:0,
    type:"",
    id:0,
    isBatteryPercentage:false,
    inverterVoltage:0,
    batteryVoltage:0,
    maxSolarVoltage:0,
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
    {isLoading&& <CreateNewSystemComponent data={data}></CreateNewSystemComponent>}
  </div>
}
