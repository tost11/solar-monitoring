import {Button, TextField} from "@mui/material";
import {useState} from "react";
import React from "react";
import {useTranslation} from "react-i18next";

export interface NamingsManagerProps {
  setNamings: (v:{[key: string]: string}) => void,
  namings: {[key: string]: string},
  doubleId: boolean
}

export default function NamingsManager({namings,setNamings,doubleId}: NamingsManagerProps) {

  const{ t } = useTranslation()

  const [editItem,setEditItem] = useState<{index:number|undefined,id1:number,id2:number,name:string}>({index:undefined,id1:0,id2:0,name:""})

  const resetEditItem=()=>{
    setEditItem({index:undefined,id1:0,id2:0,name:""})
  }

  const addItem = ()=>{
    let ret = {...namings}

    let id = ""+editItem.id1
    if(doubleId){
      id = id + "-" + editItem.id2
    }

    ret[id] = editItem.name
    setNamings(ret)
    resetEditItem()
  };

  const saveItem = ()=>{

    // @ts-ignore
    let item = Object.entries(namings)[editItem.index]

    let id = ""+editItem.id1
    if(doubleId){
      id = id + "-" + editItem.id2
    }

    let ret = {}
    Object.entries(namings).forEach(([k,v],i)=>{
      if(editItem.index == i){
        //@ts-ignore
        ret[id]=editItem.name
      }else{
        //@ts-ignore
        ret[k]=v
      }
    })

    setNamings(ret)
    resetEditItem()
  };


  const editItemSetId = (id:number,second:boolean)=>{
    if(second) {
      setEditItem({index: editItem.index, id1:editItem.id1, id2:id, name: editItem.name})
    }else{
      setEditItem({index: editItem.index, id1:id, id2:editItem.id2, name: editItem.name})
    }
  }

  const editItemSetName = (name:string)=>{
    setEditItem({index:editItem.index,id1:editItem.id1,id2:editItem.id2,name})
  }

  const deleteItem = (id:string)=>{
    let ret = {}
    Object.entries(namings).forEach(([k,v])=>{
      if(id !== k){
        //@ts-ignore
        ret[k]=v
      }
    })
    setNamings(ret)
  }

  const internalSetEditItem = (id:string,name:string,index:number)=> {
    if (doubleId) {
      let arr = id.split("-")
      setEditItem({index, id1:Number(arr[0]),id2:Number(arr[1]), name})
    } else {
      setEditItem({index, id1:Number(id), id2:0, name})
    }
  }

  return <>
    <div className="defaultFlex">
      <TextField className={"Input default-margin"} label={doubleId ? t("common.device-sub")+" Id":"Id"} variant="outlined" type={"number"} value={editItem.id1} onChange={e=>editItemSetId(Number(e.target.value),false)}/>
      {doubleId && <TextField className={"Input default-margin"} label="Id" variant="outlined" type={"number"} value={editItem.id2} onChange={e=>editItemSetId(Number(e.target.value),true)}/>}
      <TextField className={"Input default-margin"} label={t("common.name")} variant="outlined" value={editItem.name} onChange={e=>editItemSetName(e.target.value)}/>
      {editItem.index === undefined && <Button disabled={editItem.id1 < 0 || (doubleId && editItem.id2 < 0) || editItem.name === ""} onClick={addItem} variant="contained">{t("common.add")}</Button>}
      {editItem.index !== undefined && <>
          <Button onClick={saveItem} variant="contained">{t("common.save")}</Button>
          <Button onClick={()=>resetEditItem()} variant="contained">{t("common.cancel")}</Button>
        </>}
    </div>
    <div style={{marginTop: "10px",display:"flex",flexDirection:"column",gap:"10px"}}>
      {Object.entries(namings).map(([k,v],i) => {
        let arr:string[] = []
        if(doubleId){
          arr = k.split("-")
        }
        return <div key={i} className="defaultFlex" style={{alignItems:"baseline"}}>
          {doubleId ? <>DeviceId: {arr[0]}, Id: {arr[1]}, {t("common.name")}: {v}</> : <>Id: {k}, {t("common.name")}: {v}</>}
          <Button onClick={() => internalSetEditItem(k,v,i)} variant="contained">{t("common.edit")}</Button>
          <Button onClick={() => deleteItem(k)} variant="contained">{t("common.delete")}</Button>
        </div>
      })}
      </div>
    </>

}
