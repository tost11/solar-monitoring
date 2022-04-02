import React from "react";
import {CartesianGrid, Legend, Line, LineChart, Tooltip, XAxis, YAxis} from "recharts";
import moment from "moment";
import {convertToDuration} from "./TimeSelector";
import {GraphDataObject} from "./DetailDashboard";

export interface GraphProps{
  labels: string[]
  graphData:GraphDataObject
  timeRange: string;
  unit?: string;
  min?: number;
  max?: number;
}


export default function LineGraph({timeRange,graphData,unit,labels,min,max}:GraphProps) {
  const colors =["#8884d8","#ec0f0f","#68e522","#1259d5"]
  const dur = convertToDuration(timeRange);


  return <div>
    {graphData&&
    <LineChart className={"Graph"} width={450} height={200} data={graphData.data}
               margin={{top: 5, right: 30, left: 20, bottom: 5}}>
      <CartesianGrid strokeDasharray="3 3"/>
      <XAxis dataKey="time"
             domain={[dur.start.getTime(), dur.end.getTime()]}
             type='number'
             tickFormatter={(unixTime) => moment(unixTime).format('HH:mm')}/>
      <YAxis
          unit={unit?unit:undefined}
          domain={[min?min:'dataMin', max?max:'dataMax']}
      />
      <Tooltip labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD HH:mm')}/>
      <Legend/>
      {labels.map((l,index)=>{
        return <Line key={index} type="monotone" dataKey={l} stroke={colors[index]}/>
      })}


    </LineChart>
    }
  </div>

}
