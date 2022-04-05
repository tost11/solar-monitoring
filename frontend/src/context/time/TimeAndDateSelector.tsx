import * as React from "react";
import {useEffect, useState} from "react";
import moment from "moment";
import TimeSelector, {generateDurationPickerInfo, stringDurationToMilliseconds} from "./TimeSelector";

export interface TimeAndDuration{
  start: Date;
  end: Date;
  duration: number;
  durationString: string;
}

interface TimeAndDateSelectorProps{
  onChange: (timeAndDate:TimeAndDuration) =>void;
  initialTimeRange:string;
  timeRanges:string[];
  minDate?: Date;
  maxDate?: Date;
  initialDate?: Date;
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

export default function TimeAndDateSelector({onChange,initialTimeRange,timeRanges,minDate,maxDate,initialDate,onlyDate}:TimeAndDateSelectorProps) {

  const [date,setDate] = useState(initialDate?initialDate:new Date());
  const [duration, setDuration] = useState(generateDurationPickerInfo(initialTimeRange))

  const formatDate = (date:any) => {
    if(!date){
      return undefined;
    }
    if(onlyDate){
      return moment(date).format('YYYY-MM-DD')
    }else {
      return moment(date).format('YYYY-MM-DD HH:mm')
    }
  }

  useEffect(()=>{
    onChange({
      end: date,
      start: new Date(date.getTime() - duration.duration),
      duration: duration.duration,
      durationString: duration.name
    })},
  [duration,date])

  const dateChanged = (ev:any) =>{
    let date = moment(ev.target.value).toDate()
    setDate(date);
  }

  return <div>
    <div style={{display:"flex"}}>
      <TimeSelector onChange={setDuration} value={duration.name} values={timeRanges}/>
      <input style={{marginTop:"auto",marginBottom:"auto"}} type={onlyDate?"date":"datetime-local"} min={minDate ? formatDate(minDate):undefined} max={maxDate ? formatDate(maxDate):undefined} defaultValue={formatDate(date)} onChange={dateChanged}/>
    </div>
  </div>
}