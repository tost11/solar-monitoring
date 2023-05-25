import React from 'react';
import {Bar, BarChart, CartesianGrid, Cell, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import {TimeAndDuration} from "./time/TimeAndDateSelector";
import {formatDefaultValueWithUnit} from "./utils/GraphUtils";
import moment from "moment-timezone";

export interface BarGraphData{
  data:any[]
}

export interface BarGraphProps{
  graphData: BarGraphData,
  labels:string[]
  timeRange: TimeAndDuration
  unit? :string
  timezone?  :string
  colors? : string[]
  negativeColours ? : string[],
  multFactor?: number
}

export default function BarGraph({negativeColours,colors,timezone,timeRange,graphData,labels,unit,multFactor}:BarGraphProps) {

  let usedColors = ["#8884d8","#ec0f0f","#68e522","#1259d5"];
  if(colors && colors.length > 0){
    usedColors = colors
  }

  let usedNegativeColors = usedColors;
  if(negativeColours && negativeColours.length > 0){
    usedNegativeColors = negativeColours
  }

  const getColourAtIndex = (colors:string[],index:number)=>{
    if(colors.length === 0){
      return 'rgb(0,0,0)'
    }
    let i = index % colors.length
    return colors[i]
  }


  return <div>
    {graphData &&
      <ResponsiveContainer width="95%" height={200}>
        <BarChart barGap={"1%"} barCategoryGap="3%" className={"Graph"} data={graphData.data}
                   margin={{top: 5, right: 30, left: 20, bottom: 5}} >
          <CartesianGrid strokeDasharray="3 3"/>
          <XAxis dataKey="time"
                 domain={[timeRange.start.getTime(),timeRange.end.getTime()]}
                 type='number'
                 scale="time"
                 tickFormatter={(unixTime) => (timezone?moment(unixTime).tz(timezone):moment(unixTime)).format('DD.MM')}/>
          <YAxis tickFormatter={value => formatDefaultValueWithUnit(multFactor ? value*multFactor : value, unit)}/>
          <Tooltip formatter={(value: number) => formatDefaultValueWithUnit(multFactor ? value*multFactor : value, unit)} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD')}/>
          <Legend />
          {labels.map((l,index)=>{
            return <Bar fill={negativeColours ? undefined : getColourAtIndex(usedColors,index)} key={index} type="monotone" dataKey={l}>){
              graphData.data.map((entry, i) => {
                return <Cell key={i} fill={entry[l] >= 0
                  ? getColourAtIndex(usedColors,index) // green
                  : getColourAtIndex(usedNegativeColors,index)}/>
              })}</Bar>})}

        </BarChart>
      </ResponsiveContainer>
    }
  </div>

}
