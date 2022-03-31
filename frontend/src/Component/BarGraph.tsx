import React from 'react';
import {Bar, BarChart, CartesianGrid, Cell, Legend, Tooltip, XAxis, YAxis} from 'recharts';
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
   const test= [{"time":1648166400000,"Difference":null,"Produce":null,"Consumption":null},{"time":1648252800000,"Difference":null,"Produce":null,"Consumption":null},{"time":1648339200000,"Difference":null,"Produce":null,"Consumption":null},{"time":1648425600000,"Difference":null,"Produce":null,"Consumption":null},{"time":1648512000000,"Difference":60.98847999042934,"Produce":165.57459100087485,"Consumption":104.58611101044555},{"time":1648598400000,"Difference":135.6604392528534,"Produce":293.14655039045545,"Consumption":157.4861111376022},{"time":1648684800000,"Difference":139.9559919304318,"Produce":339.35321426391596,"Consumption":199.39722233348454},{"time":1648726104987,"Difference":-69.05489248169795,"Produce":164.31322593688967,"Consumption":95.25833345519163}]

  return <div>
    {graphData&&
    <BarChart className={"Graph"} width={450} height={200} data={test}
               margin={{top: 5, right: 30, left: 20, bottom: 5}} >
      <CartesianGrid strokeDasharray="3 3"/>
      <XAxis dataKey="time"
             domain={[from, to]}
             type='number'
             scale="time"
             tickFormatter={(unixTime) => moment(unixTime).format('DD.MM')}/>
      <YAxis />
      <Tooltip labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD')}/>
      <Legend/>
      {labels.map((l,index)=>{
        return <Bar key={index} type="monotone" dataKey={l} fill={colors[index]} >(
          {l=="Difference"&&
          test.map((entry, i) => {
            console.log(entry.Difference)
            return <Cell key={i} fill={entry.Difference >= 0
              ? '#089c19' // green
              : 'rgb(234,6,6)'}/>
          })})</Bar>
      })}

    </BarChart>
    }
  </div>

}
