import React, {useEffect, useState} from "react";
import {createNewToken, getSystem, SolarSystemDTO} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import CreateSystemView from "./CreateSystemView";
import {Button, Divider} from "@mui/material";
import {toast} from "react-toastify";

export default function EditSystemView() {
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
        {data.managers && <div style={{margin:"10px"}}>
          <div style={{display:"flex",flexWrap:"wrap", gap:"10px"}}>
            <div style={{marginTop:"auto",marginBottom:"auto"}}>Forget the Token ?</div>
            <Button onClick={requestNewToken}>Create a new Token</Button>
          </div>
          <Divider />
        </div>}
        <CreateSystemView data={data}/>
      </div>}
  </div>
}
