import React, {useEffect, useState} from "react";
import {createNewToken, getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import CreateNewSystemComponent from "./CreateNewSystemComponent";
import {Button} from "@mui/material";
import {toast} from "react-toastify";

export default function EditSystemComponent() {
  const [data, setData] = useState<SolarSystemDTO>()

  const params = useParams()

  useEffect(() => {
    if (!isNaN(Number(params.id))) {
      getSystem("" + params.id).then((res) => {
        setData(res)
      })
    }
  }, [])

  const requestNewToken = ()=>{
      //TODO we have to find a way to mark our api calls better
      // @ts-ignore
    createNewToken(data.id).then((response)=>{
      toast.info('New Token: '+response.token,{draggable: false,autoClose: false,closeOnClick: false})
    })
  }

  return <div>
    {data &&
      <div>
        <CreateNewSystemComponent data={data}/>
        {data.managers && <Button onClick={requestNewToken}>Create a new Token</Button>}
      </div>
    }
  </div>
}
