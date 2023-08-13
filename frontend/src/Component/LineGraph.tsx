import React from "react";
import {CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";
import moment from "moment";
import {TimeAndDuration} from "./time/TimeAndDateSelector";
import {formatDefaultValueWithUnit, getGraphColourByIndex} from "./utils/GraphUtils";
import {GraphDataObject} from "../api/GraphAPI";

export interface GraphProps{
  labels: string[]
  graphData:GraphDataObject
  timeRange: TimeAndDuration
  unit?: string
  min?: number
  max?: number
  legendOverrideValue?: string
  deviceColours?: string[]
  timezone?  :string,
  valueNameOverrides?: {[key: string]: string}
}


export default function LineGraph({valueNameOverrides,timezone,timeRange,graphData,unit,labels,min,max,legendOverrideValue,deviceColours}:GraphProps) {

  /*const tickArray = [];
  let dif = timeRange.end.valueOf() - timeRange.start.valueOf();
  for(let i = 0;i < dif;){
    tickArray.push(timeRange.start.valueOf() + i);
    i=i+dif/40;
  }*/

  const getValueNameOverrides = (key:string)=>{
    if(!valueNameOverrides){
      return key
    }
    let name = valueNameOverrides[key]
    if(name){
      return name
    }
    return key
  }

  return <div>
    {graphData &&
        <ResponsiveContainer width="95%" height={200}>
        <LineChart className={"Graph"} data={graphData.data}
                   margin={{top: 5, right: 30, left: 20, bottom: 5}}>
          <CartesianGrid strokeDasharray="3 3"/>
          {<XAxis dataKey="time"
                 //ticks={tickArray}
                 //minTickGap={15}
                 //tickCount={10}
                 domain={[timeRange.start.valueOf(), timeRange.end.valueOf()]}
                 type='number'
                 tickFormatter={(unixTime) => (timezone?moment(unixTime).tz(timezone).local(true):moment(unixTime)).format('HH:mm')}/>}
          {<YAxis
              tickFormatter={value => formatDefaultValueWithUnit(value,unit)}
              //unit={unit?unit:undefined}
              domain={[min != undefined ? min : 'dataMin' , max != undefined ? max : 'dataMax' ]}
          />}
          {<Tooltip formatter = {(value:string, name:string) => {
            return [formatDefaultValueWithUnit(Number(value),unit), getValueNameOverrides(name)]
          }} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD HH:mm')}/>}
          {legendOverrideValue ?
            <Legend content={<div>{legendOverrideValue}</div>}/>:
            <Legend formatter={(value, entry, index) => <span className="text-color-class">{getValueNameOverrides(value)}</span>}/>
          }
          {labels.map((l,index)=>{
            return <Line connectNulls={timeRange.duration < 1000 * 60 * 11} dot={false} key={index} type="monotone" dataKey={l} stroke={deviceColours?deviceColours[index]:getGraphColourByIndex(index)}/>
          })}
        </LineChart>
      </ResponsiveContainer>
    }
  </div>

}
