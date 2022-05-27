import * as React from 'react';
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import moment from "moment";

export interface DurationPickerInfo{
  duration: number;
  name: string;
}

interface RefreshTimeSelectorProps{
  onChange: (value:DurationPickerInfo) =>void;
  value:string;
  values:string[];
}

export function stringDurationToMilliseconds(selection:string):number {
  var amount = parseInt(selection.substring(0, selection.length - 1))
  if (!amount) {
    return 0;
  }
  var now = new Date();
  var unit = selection.charAt(selection.length - 1);
  var dur;
  if (unit == "s") {
    dur = moment.duration(amount, "s");
  } else if (unit == "m") {
    dur = moment.duration(amount, "m");
  } else if (unit == "h") {
    dur = moment.duration(amount, "h");
  } else if (unit == "w") {
    dur = moment.duration(amount, "w");
  } else if (unit == "M") {
    dur = moment.duration(amount, "M");
  } else if (unit == "y") {
    dur = moment.duration(amount, "y");
  } else {
    return 0;
  }
  return dur.asMilliseconds()
}

export function generateDurationPickerInfo(durationString:string){
  return {
    duration:stringDurationToMilliseconds(durationString),
    name:durationString
  }
}

export default function TimeSelector({onChange,value,values}:RefreshTimeSelectorProps){

  const handleChange = (event: SelectChangeEvent) => {
    onChange(generateDurationPickerInfo(event.target.value))
  };

return<div>
  <Box sx={{ minWidth: 120}}>
      <InputLabel id="demo-simple-select-label">TimeRange</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={value}
        label="RefreshTime"
        onChange={handleChange}
      >
        {values.map((v,k)=><MenuItem key={k} value={v}>{v}</MenuItem>)}
      </Select>
  </Box>
</div>
}
