import React, {useEffect, useState} from "react";
import {getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import {CircularProgress} from "@mui/material";

export default function EditSystemComponent() {
  const initialState = {
    name: "",
    creationDate: 0,
    type: "",
    grafanaUid: "",
    token: "",
  };
  const [data, setData] = useState<SolarSystemDTO>(initialState)
  const [isLoading, setIsLoading] = useState(false)

  const params = useParams()
  useEffect(() => {
    if (!isNaN(Number(params.id))) {
      getSystem("" + params.id).then((res) => {
        setData(res)
      }).then(() =>
        setIsLoading(true))
    }
  }, [])

  return <div>
    {isLoading ? <div style={{display: "flex", justifyContent: "center"}}>
    <p>{data.name+
        data.token+
        data.grafanaUid+
        data.type}</p>
    </div> : <CircularProgress/>}
  </div>
}
