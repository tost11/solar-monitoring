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
import {Button, Divider, TextField} from "@mui/material";
import {toast} from "react-toastify";
import ManagersOfTheSystem from "../Component/ManagersOfTheSystem";
import SetStatusList from "../Component/SetStatusList";
import TagModal from "../Component/TagModal";
import {apiAddTagToSystem, apiRemoveTagFromSystem, TagDTO} from "../api/UserAPIFunctions";
import TagView from "../Component/TagView";
import {useTranslation} from "react-i18next";

export default function EditSystemView() {

  const { t } = useTranslation();

  const [data, setData] = useState<SolarSystemDTO>()
  const [newStatusName, setNewStatusName] = useState<string>()
  const [booleanStatus, setBooleanStatus] = useState<BooleanStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(false)
  const [tagModalOpen, setTagModalOpen] = useState(false)

  const params = useParams()

  useEffect(() => {
    if (params.id) {
      getSystem(params.id).then((res) => {
        setData(res)
        if(res?.status?.booleans){
          setBooleanStatus(res.status.booleans)
        }
      })
    }
  }, [])

  const addTagToSystem = (tag:TagDTO)=>{
    apiAddTagToSystem(data?.id,tag.id).then(()=>{
      let newData = {...data} as SolarSystemDTO
      newData.tags.push(tag);
      setData(newData);
      setTagModalOpen(false)
    })
  }

  const deleteTagFromSystem = (tag:TagDTO)=>{
    apiRemoveTagFromSystem(data?.id,tag.id).then(()=>{
      let newData = {...data} as SolarSystemDTO
      let newTags = []
      for (let t of newData.tags) {
        if(t.id !== tag.id){
          newTags.push(t);
        }
      }
      newData.tags = newTags;
      setData(newData);
    })
  }

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
    setNewStatusName("")
  }

  const internalDeleteBooleanStatus = (systemId:string,status:BooleanStatus)=>{
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
          <Divider/>
          <h3>{t("views.edit_system.status_header")}</h3>
          <h4>{t("views.edit_system.status_existing")}</h4>
          <SetStatusList booleanStatus={booleanStatus} systemId={data.id} internalSetBooleanStatus={setBooleanStatus}
                         internalDeleteBooleanStatus={internalDeleteBooleanStatus} loading={statusLoading}
                         setLoading={setStatusLoading}/>
          <h4>{t("views.edit_system.status_add_header")}</h4>
          <div className="defaultFlex">
            <TextField className={"Input default-margin"} type="text" name="systemName" placeholder="SystemName"
                       label="SystemName" value={newStatusName}
                       onChange={event => setNewStatusName(event.target.value)}/>
            <Button disabled={newStatusName == undefined || newStatusName.length == 0 || statusLoading}
                    variant="contained"
                    onClick={() => {
                      setStatusLoading(true)
                      // @ts-ignore
                      addBooleanStatus(data.id, newStatusName).then(addToBooleanStatus).catch(() => {
                        setStatusLoading(false)
                      })
                    }
                    }>{t("views.edit_system.add_status")}</Button>
          </div>

          <Divider/>
          <h4>{t("views.edit_system.tag_header")}</h4>
          <TagModal addTag={addTagToSystem} open={tagModalOpen} currentTags={data.tags} onClose={()=>setTagModalOpen(false)}/>
          <TagView showDelete={true} tags={data.tags} onDelete={deleteTagFromSystem}/>
          <Button variant="outlined" onClick={()=>setTagModalOpen(true)}>{t("views.edit_system.tag_add")}</Button>

          {data.managers && <div style={{marginTop: "10px"}}>
            <Divider/>
            <h3>{t("views.edit_system.permission_header")}</h3>
            <div style={{
              backgroundColor: "whitesmoke",
              overflow: "scroll",
              maxHeight: "400px",
              width: "40%",
              justifyContent: "center"
            }}>
              <ManagersOfTheSystem initManagers={data.managers} systemId={data.id}/>
            </div>
          </div>}
        </>}
      </div>}
  </div>
}
