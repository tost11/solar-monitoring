import * as React from 'react';
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, {SelectChangeEvent} from '@mui/material/Select';

interface RefreshTimeSelectorProps{
  setTime: (value:string) =>void;
  initialValue:string;
  values:string[];
}


export default function TimeSelector({setTime,initialValue,values}:RefreshTimeSelectorProps){

  const handleChange = (event: SelectChangeEvent) => {
    setTime(event.target.value);
    console.log(event.target.value)
  };

return<div className={"RefreshTimeBox"}>
  <Box sx={{ minWidth: 120}}>
      <InputLabel id="demo-simple-select-label">RefreshTime</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={initialValue}
        label="RefreshTime"
        onChange={handleChange}
      >
        {values.map((v,k)=><MenuItem key={k} value={v}>{v}</MenuItem>)}
      </Select>
  </Box>
</div>
}
