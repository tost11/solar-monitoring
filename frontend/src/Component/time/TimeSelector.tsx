import * as React from 'react';
import {Box, FormControl, InputLabel, MenuItem, Select} from '@mui/material';
import type {SelectChangeEvent} from '@mui/material/Select';
import moment from "moment";
import {useTranslation} from "react-i18next";

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

  const { t } = useTranslation()

  const handleChange = (event: SelectChangeEvent) => {
    onChange(generateDurationPickerInfo(event.target.value))
  };

return<div>
  <Box sx={{ minWidth: 120}}>
    <FormControl fullWidth className="Input">
      <InputLabel className="Input">{t("components.time_range.duration")}</InputLabel>
      <Select
        value={value}
        label={t("components.time_range.duration")}
        onChange={handleChange}
        >
        {values.map((v,k)=><MenuItem key={k} value={v}>{v}</MenuItem>)}
      </Select>
    </FormControl>
  </Box>
</div>
}
