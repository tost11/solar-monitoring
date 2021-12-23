import React, {useContext, useState} from "react";
import {Box, Button, FormControl, Input, InputLabel, MenuItem, OutlinedInput, Theme, useTheme} from '@mui/material';
import {UserContext,Login} from "../UserContext"
import Select, { SelectChangeEvent } from '@mui/material/Select';
import { createSystem } from "../api/SolarSystemAPI";


export default function CreateNewSystemComponent() {
  const [name,setName]=useState("");
  const [type, setType] = useState("");
  let number:number
  const doNewSystem = createSystem();

  const handleChange = (event: SelectChangeEvent) => {
    setType(event.target.value as string);
  };





  return <div>
    <Input className="default-margin" type="text" name="RegisterName" placeholder="RegisterName"  value={name} onChange={event=>setName(event.target.value)}/>
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
          <MenuItem value={1}>SELFMADE</MenuItem>
          <MenuItem value={2}>SELFMADE_CONSUMPTION</MenuItem>
          <MenuItem value={3}>SELFMADE_INVERTER</MenuItem>
          <MenuItem value={4}>SELFMADE_DEVICE</MenuItem>

        </Select>
      </FormControl>
    </Box>



  </div>


}
