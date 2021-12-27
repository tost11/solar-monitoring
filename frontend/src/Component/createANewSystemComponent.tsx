import React, {useContext, useState} from "react";
import {Box, Button, FormControl, IconButton, Input, InputLabel, ListItemIcon, MenuItem, OutlinedInput, Theme, useTheme} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import Select, { SelectChangeEvent } from '@mui/material/Select';
import { createSystem,SolarSystemDTO} from "../api/SolarSystemAPI";
import TextField from '@mui/material/TextField';
import InfoIcon from '@mui/icons-material/Info';

interface IHash {
[deltails:string] :string
}


export default function CreateNewSystemComponent({}:IHash) {
  const [systemName,setsystemName]=useState("");
  const [systemType, setSystemType] = useState("");
  const [creationDate,setCreationDate]= useState("");
  const doNewSystem = createSystem();
  console.log(creationDate)
  console.log(systemType)
  let date =new Date(creationDate).getTime()/1000;
  console.log(date)


  const handleChange = (event: SelectChangeEvent) => {
    setSystemType(event.target.value as string);
  };




  return <div>
    <Input className="default-margin Input" type="text" name="systemName" placeholder="SystemName"  value={systemName} onChange={event=>setsystemName(event.target.value)}/>
    <Input className="default-margin Input" type="date" name="creationDate" placeholder="creationDate"  value={creationDate} onChange={event=>setCreationDate(event.target.value)}/>


    <Box className="SolarTypeMenuBox">
      <FormControl fullWidth  className="Input">
        <InputLabel className="Input">SolarSystemType</InputLabel>
        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={systemType}
          label="SolarSystem"
          onChange={handleChange}
        >
            <MenuItem value={"SELFMADE"}  className="menuContant" >Salfmade Solarsystem<IconButton color="primary" onClick={event=>event.stopPropagation()}><InfoIcon  color="primary"></InfoIcon></IconButton></MenuItem>
            <MenuItem value={"SELFMADE_CONSUMPTION"} >Salfmade with Consumption<IconButton color="primary" onClick={event=>event.stopPropagation()}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>
            <MenuItem value={"SELFMADE_INVERTER"} >Salfmade with inverter<IconButton color="primary" onClick={event=>event.stopPropagation()}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>
            <MenuItem value={"SELFMADE_DEVICE"} >Salfmade without converter<IconButton color="primary" onClick={event=>event.stopPropagation()}><InfoIcon color="primary"></InfoIcon></IconButton></MenuItem>

        </Select>
      </FormControl>
    </Box>

    <Button variant="outlined" onClick={() => {
      doNewSystem({name:systemName,creationDate:date,type: systemType}).then((response)=>{


      }).catch((e:Response)=>{
        e.json().then((k)=>{
        console.log(k)
        })})}
    }>Create a new SolarSystem</Button>

  </div>


}
