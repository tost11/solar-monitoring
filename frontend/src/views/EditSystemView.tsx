import React, {useEffect, useState} from "react";
import {
  addBooleanStatus,
  BooleanStatus,
  createNewToken,
  deleteBooleanStatus,
  getSystem,
  SolarSystemDTO
} from "../api/SolarSystemAPI";
import {useParams} from "react-router-dom";
import CreateSystemView from "./CreateSystemView";
import {Button, Divider, Switch, TextField, Typography} from "@mui/material";
import {toast} from "react-toastify";
import ManagersOfTheSystem from "../Component/ManagersOfTheSystem";
import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";

export default function EditSystemView() {
  const [data, setData] = useState<SolarSystemDTO>()
  const [newStatusName, setNewStatusName] = useState<string>()
  const [booleanStatus, setBooleanStatus] = useState<BooleanStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(false)

  const params = useParams()

  useEffect(() => {
    if (!isNaN(Number(params.id))) {
      getSystem("" + params.id).then((res) => {
        setData(res)
        if(res?.status?.booleans){
          setBooleanStatus(res.status.booleans)
        }
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

  const addToBooleanStatus = (newOne:BooleanStatus)=>{
    let v = [...booleanStatus]
    v.push(newOne)
    setBooleanStatus(v)
    setStatusLoading(false)
  }

  const internalDeleteBooleanStatus = (systemId:number,status:BooleanStatus)=>{
    setStatusLoading(true)
    deleteBooleanStatus(systemId,status.name).then(()=>{
      let arr: BooleanStatus[] = []
      booleanStatus.forEach(v=>{
        if(v.name !== status.name){
          arr.push(v)
        }
      })
      setBooleanStatus(arr)
      setStatusLoading(false)
    }).catch(()=>{
      setStatusLoading(false)
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
        {data && <>
          <Divider />
          <h3>Custom Status Management</h3>
          {booleanStatus.length > 0 && <div style={{display: "flex",flexDirection: "column"}}>
            <h4>Existing status</h4>
            {booleanStatus.map((v,i)=><div key={i}>
              <Typography>
                <Switch checked={v.value} onChange={() => {
                }}/>
                {v.name}
                <IconButton disabled={statusLoading} onClick={()=>internalDeleteBooleanStatus(data.id,v)}><DeleteIcon/></IconButton>
              </Typography>
            </div>)}
          </div>
          }
          <h4>Add status</h4>
          <div className="defaultFlex">
            <TextField className={"Input default-margin"} type="text" name="systemName" placeholder="SystemName" label="SystemName" value={newStatusName}
                     onChange={event => setNewStatusName(event.target.value)}/>
            <Button disabled={newStatusName == undefined || newStatusName.length == 0 || statusLoading} variant="contained"
              onClick={() => {
                setStatusLoading(true)
                addBooleanStatus(data.id,newStatusName).then(addToBooleanStatus).catch(()=>{
                  setStatusLoading(false)
                  setNewStatusName("")
                })
              }
            }>Add status</Button>
          </div>

          {data.managers && <div style={{marginTop:"10px"}}>
            <Divider />
            <h3>Permission Management</h3>
            <div style={{backgroundColor: "whitesmoke", overflow: "scroll", maxHeight: "400px", width: "40%",justifyContent:"center"}}>
              <ManagersOfTheSystem initManagers={data.managers} systemId={data.id}/>
            </div>
          </div>}
          </>}
      </div>}
  </div>
}
