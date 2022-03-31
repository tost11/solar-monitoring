import * as React from 'react';
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import moment from "moment";

interface RefreshTimeSelectorProps{
  setTime: (value:string) =>void;
  initialValue:string;
  values:string[];
}

interface Duration{
  start: Date;
  end: Date;
}

export function convertToDuration(selection:string):Duration|undefined{
  var amount = parseInt(selection.substring(0,selection.length-1))
  if(!amount){
    return undefined;
  }
  var now = new Date();
  var unit = selection.charAt(selection.length-1);
  var dur;
  if(unit == "s") {
    dur = moment.duration(amount, "s");
  }else if(unit == "m") {
    dur = moment.duration(amount, "m");
  }else if(unit == "h") {
    dur = moment.duration(amount, "h");
  }else if(unit == "w") {
    dur = moment.duration(amount, "w");
  }else if(unit == "M") {
    dur = moment.duration(amount, "M");
  }else if(unit == "y") {
    dur = moment.duration(amount, "y");
  }else{
    return undefined;
  }
  return {
    end: now,
    start: moment(now).subtract(dur).toDate()
  };
}

export default function TimeSelector({setTime,initialValue,values}:RefreshTimeSelectorProps){

  const handleChange = (event: SelectChangeEvent) => {
    setTime(event.target.value);
  };

return<div className={"RefreshTimeBox"}>
  <Box sx={{ minWidth: 120}}>
      <InputLabel id="demo-simple-select-label">TimeRange</InputLabel>
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
