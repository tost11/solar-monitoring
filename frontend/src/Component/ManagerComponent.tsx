import React, {useContext, useEffect} from "react";
import {deleteMangerRelation, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import type { SelectChangeEvent } from "@mui/material/Select";
import DeleteIcon from '@mui/icons-material/Delete';
import {UserContext} from "../context/UserContext";
import {useTranslation} from "react-i18next";

interface ManagerComponentProps{
  manager:ManagerDTO
  systemId:string
  setListOfManagers:(managerDTOS:ManagerDTO[])=>void
}
export default function ManagerComponent({manager,systemId,setListOfManagers}:ManagerComponentProps){

  var  { t } = useTranslation()

  const [role, setRole] = React.useState(manager.role)
  const login = useContext(UserContext);

  useEffect(()=>{
    if(role!=manager.role) {
      setManageUser({id:manager.id, systemId:systemId, role:role}).then(r=>setListOfManagers(r))
    }
  },[role])

  const deleteManager = () => {
    deleteMangerRelation(manager.id,systemId).then((r)=>setListOfManagers(r.managers))
  }
  return(<div className={login?.id===manager.id?"default-margin  DisabledMangerList":"default-margin ManagerListElement"}>
    <div style={{justifySelf:"flex-start",width:"90%", flexDirection:"row" ,display:"flex"}} className={"default-margin"}>
      <h1 >{manager.userName}</h1>
      {login?.id===manager.id&&<h3 style={{alignSelf:"flex-end" ,color:"red"}}>{t("components.manager.you")}</h3>}
    </div>

    <RolePicker role={role} setRole={setRole}/>
    <IconButton onClick={()=>deleteManager()}><DeleteIcon/></IconButton>

  </div>)
}
interface RolePickerProps{
  role:string
  setRole:(value:string)=>void
}

function RolePicker ({role,setRole}:RolePickerProps){

  var { t } = useTranslation()

  const handleChange = (event: SelectChangeEvent) => {
    setRole(event.target.value)
  }

  return <Box>
    <InputLabel id="demo-simple-select-label">{t("components.manager.permissions")}</InputLabel>
    <Select
      labelId="demo-simple-select-label"
      id="demo-simple-select"
      value={role}
      label="select role"
      onChange={handleChange}
    >

      <MenuItem value={"VIEW"}>View</MenuItem>
      <MenuItem value={"MANAGE"}>Manage</MenuItem>
      <MenuItem value={"ADMIN"}>Admin</MenuItem>
    </Select>
  </Box>

}
