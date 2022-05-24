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
  onChange: (timeAndDate:TimeAndDuration,explicitNow:boolean,fromDurationChange:boolean) =>void;
  timeRanges:string[];
  minDate?: Date;
  maxDate?: Date;
  timeRange: TimeAndDuration
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

export default function TimeAndDateSelector({timezone,onChange,timeRanges,minDate,maxDate,timeRange,onlyDate}:TimeAndDateSelectorProps) {

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
    var start = new Date(date.getTime() - timeRange.duration)
    var end = date
    if(timezone){
      start = addUtcOffsetToTime(start,timezone,false)
      end = addUtcOffsetToTime(end,timezone,false)
    }
    onChange({
      end: end,
      start: start,
      duration: timeRange.duration,
      durationString: timeRange.durationString,
    },nowButton,false)
  }

  const durationChanged = (dur:DurationPickerInfo) =>{
    var start = new Date(timeRange.end.getTime() - dur.duration)
    var end = timeRange.end
    if(timezone){
      start = addUtcOffsetToTime(start,timezone,false)
      end = addUtcOffsetToTime(end,timezone,false)
    }
    onChange({
      end: end,
      start: start,
      duration: dur.duration,
      durationString: dur.name
    },false,true)
  }

  return <div>
    <div style={{display:"flex"}}>
      <TimeSelector onChange={durationChanged} value={timeRange.durationString} values={timeRanges}/>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        {onlyDate?
          <DatePicker
            renderInput={(props) => <TextField {...props} />}
            label="DateTimePicker"
            value={timeZoneTimeRangeFix(timeRange.end)}
            minDate={minDate?moment(timeZoneTimeRangeFix(minDate)):undefined}
            maxDate={maxDate?moment(timeZoneTimeRangeFix(maxDate)):undefined}
            clearable={true}
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
            maxDateTime={maxDate?(moment(timeZoneTimeRangeFix(maxDate))):undefined}
            clearable={true}
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
