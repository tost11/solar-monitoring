import * as React from 'react';
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {useEffect} from "react";
import {MenuList} from "@mui/material";

interface RefreshTimeSelectorProps{
  setRefreshTime: (value:string) =>void;
  refreshTime:string
}


export default function RefreshTimeSelector({setRefreshTime,refreshTime}:RefreshTimeSelectorProps){
  const [time,setTime]=React.useState(refreshTime);

  const handleChange = (event: SelectChangeEvent) => {
    setTime(event.target.value);
    setRefreshTime(event.target.value);

  };

return<div className={"RefreshTimeBox"}>
  <Box sx={{ minWidth: 120}}>
      <InputLabel id="demo-simple-select-label">RefreshTime</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={time}
        label="RefreshTime"
        onChange={handleChange}
      >
        <MenuItem value={"5s"}>5s</MenuItem>
        <MenuItem value={"10s"}>10s</MenuItem>
        <MenuItem value={"20s"}>20s</MenuItem>
        <MenuItem value={"30s"}>30s</MenuItem>
        <MenuItem value={"1m"}>1m</MenuItem>
        <MenuItem value={"5m"}>5m</MenuItem>
        <MenuItem value={"15m"}>15m</MenuItem>
        <MenuItem value={"30m"}>30m</MenuItem>
        <MenuItem value={"1h"}>1h</MenuItem>
        <MenuItem value={"2h"}>2h</MenuItem>
        <MenuItem value={"1d"}>1d</MenuItem>
      </Select>
  </Box>
</div>
}
