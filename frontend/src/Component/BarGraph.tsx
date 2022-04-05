import React from 'react';
import {Bar, BarChart, CartesianGrid, Cell, Legend, Tooltip, XAxis, YAxis} from 'recharts';
import moment from "moment";
import {GraphDataObject} from "./DetailDashboard";
import {TimeAndDuration} from "../context/time/TimeAndDateSelector";

export interface BarGraphProps{
  graphData:GraphDataObject
  labels:string[]
  timeRange: TimeAndDuration
  unit? :string
}

export default function BarGraph({timeRange,graphData,labels,unit}:BarGraphProps) {
  const colors =["#8884d8","#ec0f0f","#68e522","#1259d5"]
  return <div>
    {graphData&&
    <BarChart className={"Graph"} width={450} height={200} data={graphData.data}
               margin={{top: 5, right: 30, left: 20, bottom: 5}} >
      <CartesianGrid strokeDasharray="3 3"/>
      <XAxis dataKey="time"
             domain={[timeRange.start.getTime(),timeRange.end.getTime()]}
             type='number'
             scale="time"
             tickFormatter={(unixTime) => moment(unixTime).format('DD.MM')}/>
      <YAxis unit={unit?unit:undefined}/>
      <Tooltip labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD')}/>
      <Legend/>
      {labels.map((l,index)=>{
        return <Bar key={index} type="monotone" dataKey={l} fill={colors[index]} >(
          {l=="Difference"&&
          graphData.data.map((entry, i) => {
            return <Cell key={i} fill={entry.Difference >= 0
              ? '#089c19' // green
              : 'rgb(234,6,6)'}/>
          })})</Bar>
      })}

    </BarChart>
    }
  </div>

}
