import React from 'react';
import {Bar, BarChart, CartesianGrid, Legend, Tooltip, XAxis, YAxis} from 'recharts';
import moment from "moment";
import {GraphDataObject} from "./DetailDashboard";

export interface BarGraphProps{
  graphData:GraphDataObject
  labels:string[]
  from: number
  to:number

}

export default function BarGraph({from,to,graphData,labels}:BarGraphProps) {
  const colors =["#8884d8","#ec0f0f","#68e522","#1259d5"]

  return <div>
    {graphData&&
    <BarChart className={"Graph"} width={450} height={200} data={graphData.data}
               margin={{top: 5, right: 30, left: 20, bottom: 5}} s>
      <CartesianGrid strokeDasharray="3 3"/>
      <XAxis dataKey="time"
             domain={[from, to]}
             type='number'
             scale="time"
             tickFormatter={(unixTime) => moment(unixTime).format('DD.MM')}/>
      <YAxis />
      <Tooltip/>
      <Legend/>
      {labels.map((l,index)=>{
        return <Bar key={index} type="monotone" dataKey={l} fill={colors[index]}/>
      })}

    </BarChart>
    }
  </div>

}
