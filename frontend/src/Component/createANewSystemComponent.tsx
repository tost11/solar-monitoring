import React, {useContext, useState} from "react";
import {Box, Button, FormControl, Input, InputLabel, MenuItem, OutlinedInput, Theme, useTheme} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import Select, { SelectChangeEvent } from '@mui/material/Select';
import { createSystem,SolarSystemDTO} from "../api/SolarSystemAPI";
import TextField from '@mui/material/TextField';




export default function CreateNewSystemComponent() {
  const [name,setName]=useState("");
  const [type, setType] = useState("");
  const [creationDate,setCreationDate]= useState(0);
  const doNewSystem = createSystem();

  const handleChange = (event: SelectChangeEvent) => {
    setType(event.target.value as string);
  };



  return <div>
    <Input className="default-margin" type="text" name="RegisterName" placeholder="Name"  value={name} onChange={event=>setName(event.target.value)}/>
    <Input className="default-margin" type="date" name="RegisterName" placeholder="Name"  value={name} onChange={event=>setName(event.target.value)}/>


    <Box sx={{ minWidth: 120, maxWidth:200, mt:10}}>
      <FormControl fullWidth>
        <InputLabel>SolarSystemType</InputLabel>
        <Select
          labelId="demo-simple-select-label"
          id="demo-simple-select"
          value={type}
          label="Age"
          onChange={handleChange}
        >
          <MenuItem value={1}>Salfmade Solarsystem</MenuItem>
          <MenuItem value={2}>Salfmade with Consumption</MenuItem>
          <MenuItem value={3}>Salfmade with inverter</MenuItem>
          <MenuItem value={4}>Salfmade without converter</MenuItem>

        </Select>
      </FormControl>
    </Box>

    <Button variant="outlined" onClick={() => {
      doNewSystem({name,creationDate,type}).then((response)=>{


      }).catch((e:Response)=>{
        e.json().then((k)=>{
        console.log(k)
        })})}
    }>Create a new SolarSystem</Button>

  </div>


}
