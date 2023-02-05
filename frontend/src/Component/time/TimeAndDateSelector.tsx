import * as React from "react";
import moment from "moment";
import TimeSelector, {DurationPickerInfo, stringDurationToMilliseconds} from "./TimeSelector";
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import {Button, TextField} from "@mui/material";

export interface TimeAndDuration{
  start: Date;
  end: Date;
  duration: number;
  durationString: string;
}

export interface TimeRangeStatus{
  autoUpdate: boolean;
  time:TimeAndDuration;
}

interface TimeAndDateSelectorProps{
  onChange: (time:TimeRangeStatus) =>void;
  timeRanges:string[];
  minDate?: Date;
  timeRange: TimeRangeStatus;
  onlyDate?: boolean;
  timezone?: string;
}

export function generateTimeDuration(duration:string,date:Date){
  let dur = stringDurationToMilliseconds(duration);
  return {
    end: date,
    start: new Date(date.getTime() - dur),
    duration: dur,
    durationString: duration
  }
}

export default function TimeAndDateSelector({timezone,onChange,timeRanges,minDate,timeRange,onlyDate}:TimeAndDateSelectorProps) {

  const addUtcOffsetToTime = (date:Date,timezone:string,add:boolean,)=>{
    var utcOffset = moment().tz(timezone).utcOffset();
    utcOffset -= moment(date).utcOffset();
    if(add) {
      return moment(date).add(utcOffset, "minutes").toDate()
    }else{
      return moment(date).subtract(utcOffset, "minutes").toDate()
    }
  }


  const timeZoneTimeRangeFix = (date:Date) => {
    if(timezone) {
      return addUtcOffsetToTime(timeRange.start,timezone, true)
    }
    return date;
  }


  const dateChanged = (date:Date,nowButton:boolean) =>{
    var start = new Date(date.getTime() - timeRange.time.duration)
    var end = date
    if(timezone){
      start = addUtcOffsetToTime(start,timezone,false)
      end = addUtcOffsetToTime(end,timezone,false)
    }
    onChange({time:{
      end: end,
      start: start,
      duration: timeRange.time.duration,
      durationString: timeRange.time.durationString,
    },autoUpdate:nowButton})
  }

  const durationChanged = (dur:DurationPickerInfo) =>{
    var start = new Date(timeRange.time.end.getTime() - dur.duration)
    var end = timeRange.time.end
    if(timezone){
      start = addUtcOffsetToTime(start,timezone,false)
      end = addUtcOffsetToTime(end,timezone,false)
    }
    onChange({time:{
      end: end,
      start: start,
      duration: dur.duration,
      durationString: dur.name
    },autoUpdate:false})
  }

  return <div>
    <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
      <TimeSelector onChange={durationChanged} value={timeRange.time.durationString} values={timeRanges}/>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        {onlyDate?
          <DatePicker
            renderInput={(props) => <TextField {...props} />}
            label="DatePicker"
            value={timeZoneTimeRangeFix(timeRange.time.end)}
            minDate={minDate?moment(timeZoneTimeRangeFix(minDate)):undefined}
            maxDate={moment().add(1,"minutes")}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue._d,false)
            }}/>:
          <DateTimePicker
            renderInput={(props) => <TextField {...props} />}
            label="DateTimePicker"
            value={timeZoneTimeRangeFix(timeRange.end)}
            ampm={false}
            minDateTime={minDate?(moment(timeZoneTimeRangeFix(minDate))):undefined}
            maxDateTime={moment().add(1,"minutes")}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue._d,false)
            }}
        />}
      </div>
      <Button onClick={()=>dateChanged(timeZoneTimeRangeFix(moment().toDate()),true)}>now</Button>
    </div>
  </div>
}
