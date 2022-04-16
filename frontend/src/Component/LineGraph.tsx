import React from "react";
import {CartesianGrid, Legend, Line, LineChart, Tooltip, XAxis, YAxis} from "recharts";
import moment from "moment";
import {GraphDataObject} from "./DetailDashboard";
import {TimeAndDuration} from "../context/time/TimeAndDateSelector";
import {formatDefaultValueWithUnit, getGraphColourByIndex} from "./utils/GraphUtils";

export interface GraphProps{
  labels: string[]
  graphData:GraphDataObject
  timeRange: TimeAndDuration;
  unit?: string;
  min?: number;
  max?: number;
  legendOverrideValue?: string
  deviceColours?: string[]
}


export default function LineGraph({timeRange,graphData,unit,labels,min,max,legendOverrideValue,deviceColours}:GraphProps) {

  return <div>
    {graphData&&
      <LineChart className={"Graph"} width={450} height={200} data={graphData.data}
                 margin={{top: 5, right: 30, left: 20, bottom: 5}}>
        <CartesianGrid strokeDasharray="3 3"/>
        <XAxis dataKey="time"
               domain={[timeRange.start.getTime(), timeRange.end.getTime()]}
               type='number'
               tickFormatter={(unixTime) => moment(unixTime).format('HH:mm')}/>
        <YAxis
            tickFormatter={value => formatDefaultValueWithUnit(value,unit)}
            //unit={unit?unit:undefined}
            domain={[min != undefined ? min : 'dataMin' , max != undefined ? max : 'dataMax' ]}
        />
        <Tooltip formatter={(value: number) => formatDefaultValueWithUnit(value,unit)} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD HH:mm')}/>
        {legendOverrideValue ?
          <Legend content={<div>{legendOverrideValue}</div>}/>:
          <Legend/>}
        {labels.map((l,index)=>{
          return <Line connectNulls={timeRange.duration < 1000 * 60 * 11} dot={false} key={index} type="monotone" dataKey={l} stroke={deviceColours?deviceColours[index]:getGraphColourByIndex(index)}/>
        })}
      </LineChart>
    }
  </div>

}
