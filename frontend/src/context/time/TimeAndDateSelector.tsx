import * as React from "react";
import moment from "moment";
import TimeSelector, {DurationPickerInfo, stringDurationToMilliseconds} from "./TimeSelector";
import {DatePicker, DateTimePicker} from "@mui/lab";
import {Button, TextField} from "@mui/material";

export interface TimeAndDuration{
  start: Date;
  end: Date;
  duration: number;
  durationString: string;
}

interface TimeAndDateSelectorProps{
  onChange: (timeAndDate:TimeAndDuration) =>void;
  timeRanges:string[];
  minDate?: Date;
  maxDate?: Date;
  timeRange: TimeAndDuration
  onlyDate?: boolean;
}

export function generateTimeDuration(duration:string,date:Date){
  let dur = stringDurationToMilliseconds(duration);
  return {
    end: date,
    start: new Date(date.getTime() - dur),
    duration: dur,
    durationString: duration,
  }
}

export default function TimeAndDateSelector({onChange,timeRanges,minDate,maxDate,timeRange,onlyDate}:TimeAndDateSelectorProps) {

  const dateChanged = (date:Date) =>{
    onChange({
      end: date,
      start: new Date(date.getTime() - timeRange.duration),
      duration: timeRange.duration,
      durationString: timeRange.durationString
    })
  }

  const durationChanged = (dur:DurationPickerInfo) =>{
    onChange({
      end: timeRange.end,
      start: new Date(timeRange.end.getTime() - dur.duration),
      duration: dur.duration,
      durationString: dur.name
    })
  }

  return <div>
    <div style={{display:"flex"}}>
      <TimeSelector onChange={durationChanged} value={timeRange.durationString} values={timeRanges}/>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        {onlyDate?
            <DatePicker
            renderInput={(props) => <TextField {...props} />}
            label="DateTimePicker"
            value={timeRange.end}
            minDate={minDate?moment(minDate):undefined}
            maxDate={maxDate?moment(maxDate):undefined}
            clearable={true}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue._d)
            }}/>:
        <DateTimePicker
            renderInput={(props) => <TextField {...props} />}
            label="DateTimePicker"
            value={timeRange.end}
            ampm={false}
            minDateTime={minDate?moment(minDate):undefined}
            maxDateTime={maxDate?moment(maxDate):undefined}
            clearable={true}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue._d)
            }}
        />}
      </div>
      <Button onClick={()=>dateChanged(new Date())}>now</Button>
    </div>
  </div>
}