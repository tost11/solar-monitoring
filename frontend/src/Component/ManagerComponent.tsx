import React, {useContext, useEffect} from "react";
import {deleteMangerRelation, ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import DeleteIcon from '@mui/icons-material/Delete';
import IconButton from '@mui/material/IconButton';
import {UserContext} from "../context/UserContext";

interface ManagerComponentProps{
  manager:ManagerDTO
  systemId:number
  setListOfManagers:(managerDTOS:ManagerDTO[])=>void
}
export default function ManagerComponent({manager,systemId,setListOfManagers}:ManagerComponentProps){
  const [role, setRole] = React.useState(manager.role)
  const login = useContext(UserContext);
  useEffect(()=>{
    if(role!=manager.role) {
      setManageUser({id:manager.id, systemId:systemId, role:role})
    }
  },[role])

  const deleteManager = () => {
    deleteMangerRelation(manager.id,systemId).then((r)=>setListOfManagers(r.managers))
  }
  console.log(login?.id)
  return(<div className={login?.id===manager.id?"default-margin  DisabledMangerList":"default-margin ManagerListElement"}>
    <div style={{justifySelf:"flex-start",width:"90%", flexDirection:"row" ,display:"flex"}} className={"default-margin"}>
      <h1 >{manager.userName}</h1>
      {login?.id===manager.id&&<h3 style={{alignSelf:"flex-end" ,color:"red"}}>  It's you</h3>}
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


  const handleChange = (event: SelectChangeEvent) => {
    setRole(event.target.value)
  }

  return <Box>
    <InputLabel id="demo-simple-select-label">Select Role</InputLabel>
    <Select
      labelId="demo-simple-select-label"
      id="demo-simple-select"
      value={role}
      label="Select Role"
      onChange={handleChange}
    >

      <MenuItem value={"VIEW"}>View</MenuItem>
      <MenuItem value={"MANAGE"}>Manage</MenuItem>
      <MenuItem value={"ADMIN"}>Admin</MenuItem>
    </Select>
  </Box>

}
