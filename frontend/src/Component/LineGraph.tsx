import React from "react";
import {CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";
import moment from "moment";
import {TimeAndDuration} from "./time/TimeAndDateSelector";
import {formatDefaultValueWithUnit, getGraphColourByIndex} from "./utils/GraphUtils";
import {GraphDataObject} from "../api/GraphAPI";

export interface GraphProps{
  labels: string[]
  defaultDurations?: [number|undefined]
  defaultDuration?: number
  graphData:GraphDataObject
  timeRange: TimeAndDuration
  unit?: string
  min?: number
  max?: number
  legendOverrideValue?: string
  deviceColours?: string[]
  timezone?  :string,
  valueNameOverrides?: {[key: string]: string}
  calculatedFields?: {[labelName: string]: (dataPoint: any) => number | null}
}


export default function LineGraph({defaultDuration,valueNameOverrides,timezone,timeRange,graphData,unit,labels,min,max,legendOverrideValue,deviceColours,defaultDurations,calculatedFields}:GraphProps) {

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

  // Apply calculated fields if provided
  const augmentedData = calculatedFields ?
    graphData.data.map(point => {
      const newPoint = { ...point };
      Object.keys(calculatedFields).forEach(fieldName => {
        newPoint[fieldName] = calculatedFields[fieldName](point);
      });
      return newPoint;
    })
    : graphData.data;

  const augmentedGraphData = {
    ...graphData,
    data: augmentedData
  };

  return <div>
    {graphData &&
        <ResponsiveContainer width="95%" height={200}>
        <LineChart className={"Graph"} data={augmentedGraphData.data}
                   margin={{top: 5, right: 30, left: 20, bottom: 5}}>
          <CartesianGrid strokeDasharray="3 3"/>
          {<XAxis dataKey="time"
                 //ticks={tickArray}
                 //minTickGap={15}
                 //tickCount={10}
                 domain={[timeRange.start.valueOf(), timeRange.end.valueOf()]}
                 type='number'
                 tickFormatter={(unixTime) => (timezone?moment(unixTime).tz(timezone):moment(unixTime)).format('HH:mm')}/>}
          {<YAxis
              tickFormatter={value => formatDefaultValueWithUnit(value,unit)}
              //unit={unit?unit:undefined}
              domain={[min != undefined ? min : 'dataMin' , max != undefined ? max : 'dataMax' ]}
          />}
          {<Tooltip formatter = {(value:any, name:string) => {
            if (value === undefined || value === null) return ['', ''];
            return [formatDefaultValueWithUnit(Number(value),unit), getValueNameOverrides(name)]
          }} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD HH:mm')}/>}
          {legendOverrideValue ?
            <Legend content={() => <div>{legendOverrideValue}</div>}/>:
            <Legend formatter={(value, _entry, _index) => <span>{getValueNameOverrides(value)}</span>}/>
          }
          {labels.map((l,index)=>{
            //console.log(defaultDurations)
            let graphStep = (timeRange.duration / 1000 / augmentedGraphData.data.length)
            let durToUse = undefined;
            if(defaultDurations && defaultDurations[index]){
              durToUse = defaultDurations[index];
            }
            if(durToUse == undefined && defaultDuration){
              durToUse = defaultDuration;
            }
            let calcUse = (durToUse ? (durToUse) / 2 : 30 / 2) * 1.2
            //console.log(graphStep," and ",calcUse)
            return <Line connectNulls={graphStep < calcUse} dot={false} key={index} type="monotone" dataKey={l} stroke={deviceColours?deviceColours[index]:getGraphColourByIndex(index)}/>
          })}
        </LineChart>
      </ResponsiveContainer>
    }
  </div>

}
