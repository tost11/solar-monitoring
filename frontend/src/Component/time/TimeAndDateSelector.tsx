import * as React from "react";
import moment, {Moment} from "moment";
import TimeSelector, {DurationPickerInfo, stringDurationToMilliseconds} from "./TimeSelector";
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import {Button, TextField} from "@mui/material";
import {addUtcOffsetToTime} from "../utils/GraphUtils";

export interface TimeAndDuration{
  start: Moment;
  end: Moment;
  duration: number;
  durationString: string;
}

export interface TimeRangeStatus{
  autoUpdate: boolean;
  time:TimeAndDuration;
}

interface TimeAndDateSelectorProps{
  onChange: (time:TimeRangeStatus,fromNowButton:boolean) =>void;
  timeRanges:string[];
  minDate?: Moment;
  timeRange: TimeRangeStatus;
  onlyDate?: boolean;
  timezone?: string;
}

export function generateTimeDuration(duration:string,date:Moment){
  let dur = stringDurationToMilliseconds(duration);
  return {
    end: moment(date),
    start: moment(date.valueOf() - dur),
    duration: dur,
    durationString: duration
  }
}

export default function TimeAndDateSelector({timezone,onChange,timeRanges,minDate,timeRange,onlyDate}:TimeAndDateSelectorProps) {

  /*const timeZoneTimeRangeFix = (date:Date) => {
    if(timezone) {
      return addUtcOffsetToTime(timeRange.time.start,timezone, true)
    }
    return date;
  }*/


  const dateChanged = (date:Moment,nowButton:boolean) =>{
    let useDate = date;
    if(timezone){
      useDate = date.utc(true)
    }

    console.log("changed date is: ",useDate)

    onChange({time:{
      end: moment(useDate),
      start: moment(useDate.valueOf() - timeRange.time.duration),
      duration: timeRange.time.duration,
      durationString: timeRange.time.durationString,
    },autoUpdate:nowButton},nowButton)
  }

  const durationChanged = (dur:DurationPickerInfo) =>{
    onChange({time:{
      end: moment(timeRange.time.end),
      start: moment(timeRange.time.end.valueOf() - dur.duration),
      duration: dur.duration,
      durationString: dur.name
    },autoUpdate:timeRange.autoUpdate},false)
  }

  return <div>
    <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap"}}>
      <TimeSelector onChange={durationChanged} value={timeRange.time.durationString} values={timeRanges}/>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        {onlyDate?
          <DatePicker
            renderInput={(props) => <TextField {...props} />}
            label="DatePicker"
            value={timezone ? timeRange.time.end.clone().tz(timezone).local(true):timeRange.time.end}
            //minDate={minDate?moment(timeZoneTimeRangeFix(minDate)):undefined}
            maxDate={moment().add(1,"minutes").tz(timezone,false).local(true)}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue,false)
            }}/>:
          <DateTimePicker
            renderInput={(props) => <TextField {...props} />}
            label="DateTimePicker"
            value={timezone ? timeRange.time.end.clone().tz(timezone).local(true):timeRange.time.end}
            ampm={false}
            //minDateTime={minDate?(moment(timeZoneTimeRangeFix(minDate))):undefined}
            maxDateTime={moment().add(1,"minutes").tz(timezone).local(true)}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue,false)
            }}
        />}
      </div>
      <Button onClick={()=>dateChanged(moment().tz(timezone).local(true),true)}>now</Button>
    </div>
  </div>
}
