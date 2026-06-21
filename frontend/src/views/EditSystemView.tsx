import React, {useEffect, useState} from "react";
import {
  addBooleanStatus,
  BooleanStatus,
  createNewToken,
  deleteBooleanStatus,
  EditSolarSystemDTO,
  getSystemForEdit
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

type EditSystemData = EditSolarSystemDTO & {id: string, managers?: any, status?: any, tags?: TagDTO[]};

export default function EditSystemView() {
  const { t } = useTranslation();

  const [data, setData] = useState<EditSystemData | undefined>()
  const [newStatusName, setNewStatusName] = useState<string>()
  const [booleanStatus, setBooleanStatus] = useState<BooleanStatus[]>([])
  const [statusLoading, setStatusLoading] = useState(false)
  const [tagModalOpen, setTagModalOpen] = useState(false)

  const params = useParams()

  useEffect(() => {
    if (params.id) {
      console.log("Fetching system for edit with id:", params.id)
      getSystemForEdit(params.id).then((res) => {
        console.log("EditSystemView received data:", res)
        console.log("Data type:", typeof res)
        console.log("Data keys:", res ? Object.keys(res) : "no data")
        setData(res as any)
        if((res as any)?.status?.booleans){
          setBooleanStatus((res as any).status.booleans)
        }
      }).catch(err => {
        console.error("Error loading system for edit:", err)
        console.error("Error details:", err.message, err.stack)
      })
    }
  }, [])

  const addTagToSystem = (tag:TagDTO)=>{
    apiAddTagToSystem(data?.id,tag.id).then(()=>{
      let newData = {...data} as any
      if (!newData.tags) newData.tags = []
      newData.tags.push(tag);
      setData(newData);
      setTagModalOpen(false)
    })
  }

  const deleteTagFromSystem = (tag:TagDTO)=>{
    apiRemoveTagFromSystem(data?.id,tag.id).then(()=>{
      let newData = {...data} as any
      let newTags = []
      for (let t of newData.tags || []) {
        if(t.id !== tag.id){
          newTags.push(t);
        }
      }
      newData.tags = newTags;
      setData(newData);
    })
  }

  const requestNewToken = ()=>{
    if (!data?.id) return;
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

  if (!data) {
    return <div>Loading...</div>
  }

  return (
    <div>
      {data.managers && (
        <div style={{margin:"10px"}}>
          <div style={{display:"flex",flexWrap:"wrap", gap:"10px"}}>
            <div style={{marginTop:"auto",marginBottom:"auto"}}>Forget the Token ?</div>
            <Button onClick={requestNewToken}>Create a new Token</Button>
          </div>
          <Divider />
        </div>
      )}

      <CreateSystemView data={data}/>

      <Divider/>
      <h3>{t("views.edit_system.status_header")}</h3>
      <h4>{t("views.edit_system.status_existing")}</h4>
      <SetStatusList
        booleanStatus={booleanStatus}
        systemId={data.id}
        internalSetBooleanStatus={setBooleanStatus}
        internalDeleteBooleanStatus={internalDeleteBooleanStatus}
        loading={statusLoading}
        setLoading={setStatusLoading}
      />

      <h4>{t("views.edit_system.status_add_header")}</h4>
      <div className="defaultFlex">
        <TextField
          className={"Input default-margin"}
          type="text"
          name="systemName"
          label={t("views.edit_system.tag_label")}
          value={newStatusName || ""}
          onChange={event => setNewStatusName(event.target.value)}
        />
        <Button
          disabled={newStatusName == undefined || newStatusName.length == 0 || statusLoading}
          variant="contained"
          onClick={() => {
            setStatusLoading(true)
            addBooleanStatus(data.id, newStatusName!).then(addToBooleanStatus).catch(() => {
              setStatusLoading(false)
            })
          }}
        >
          {t("views.edit_system.status_add")}
        </Button>
      </div>

      <Divider/>
      <h4>{t("views.edit_system.tag_header")}</h4>
      <TagModal
        addTag={addTagToSystem}
        open={tagModalOpen}
        currentTags={data.tags || []}
        onClose={()=>setTagModalOpen(false)}
      />
      <TagView showDelete tags={data.tags || []} onDelete={deleteTagFromSystem}/>
      <Button variant="outlined" onClick={()=>setTagModalOpen(true)}>
        {t("views.edit_system.tag_add")}
      </Button>

      {data.managers && (
        <div style={{marginTop: "10px"}}>
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
        </div>
      )}
    </div>
  )
}
