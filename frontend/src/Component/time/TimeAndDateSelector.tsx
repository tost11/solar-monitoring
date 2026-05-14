import * as React from "react";
import moment, {Moment} from "moment";
import TimeSelector, {DurationPickerInfo, stringDurationToMilliseconds} from "./TimeSelector";
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import {Button, TextField} from "@mui/material";
import {useTranslation} from "react-i18next";

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

  const { t } = useTranslation()

  const dateChanged = (date:Moment,nowButton:boolean) =>{
    console.log("changed date is: ",date)

    onChange({time:{
      end: moment(date),
      start: moment(date.valueOf() - timeRange.time.duration),
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
    <div style={{display:"flex",flexDirection:"row", flexWrap:"wrap",rowGap:"10px", columnGap:"5px"}}>
      <TimeSelector onChange={durationChanged} value={timeRange.time.durationString} values={timeRanges}/>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        {onlyDate?
          <DatePicker
            label={t("components.date_selector.date")}
            value={timezone ? timeRange.time.end.clone().tz(timezone):timeRange.time.end}
            maxDate={timezone ? moment.tz(timezone).add(1,"minutes"): moment().add(1,"minutes")}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue,false)
            }}/>:
          <DateTimePicker
            label={t("components.date_selector.date")}
            value={timezone ? timeRange.time.end.clone().tz(timezone):timeRange.time.end}
            ampm={false}
            maxDateTime={timezone ? moment.tz(timezone).add(1,"minutes"): moment().add(1,"minutes")}
            onChange={(newValue) => {
              // @ts-ignore
              dateChanged(newValue,false)
            }}
        />}
      </div>
      <div style={{marginTop:"auto",marginBottom:"auto"}}>
        <Button onClick={()=>dateChanged(timezone?moment.tz(timezone):moment(),true)}>{t("components.date_selector.now")}</Button>
      </div>
    </div>
  </div>
}
