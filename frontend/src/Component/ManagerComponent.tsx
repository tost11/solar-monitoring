import React, {useEffect} from "react";
import {ManagerDTO, setManageUser} from "../api/SolarSystemAPI";
import {Box, InputLabel, MenuItem} from "@mui/material";
import Select, {SelectChangeEvent} from "@mui/material/Select";
import {DashboardRange} from "./Accordions/StatisticsAccordion";

interface ManagerComponentProps{
  manager:ManagerDTO
  systemId:number
}
export default function ManagerComponent({manager,systemId}:ManagerComponentProps){
  const [role, setRole] = React.useState(manager.role)
  useEffect(()=>{
    if(role!=manager.role) {
      setManageUser(manager.id, systemId, role).then(r => {
        console.log(r)
      })
    }
  },[role])

  return<div style={{display:"flex",justifyContent:"center", backgroundColor:"lightgray"}}className={"default-margin"}>
    <h1 style={{justifySelf:"flex-start",width:"90%"}} className={"default-margin"}>{manager.userName}</h1><RolePicker role={role} setRole={setRole}/>


  </div>
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
