import React from 'react';
import {Bar, BarChart, CartesianGrid, Cell, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts';
import {TimeAndDuration} from "./time/TimeAndDateSelector";
import {formatDefaultValueWithUnit, getGraphColourByIndex} from "./utils/GraphUtils";
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
  multFactor?: number,
  valueNameOverrides?: {[key: string]: string}
}

export default function DayBarGraph({valueNameOverrides,colors,timezone,timeRange,graphData,labels,unit,multFactor}:BarGraphProps) {

  //console.log("Graph data day: ",graphData)
  //console.log("Graph data timerange: ",timeRange)

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

  let realData = graphData.data;

  if(timezone) {
    realData = []

    let dataSet = new Map(graphData.data.map(item => [moment(item.time).tz(timezone).local(true).valueOf(), item]));

    let localEnd = moment(timeRange.end).tz(timezone).local(true).startOf("day")
    let localStart = moment(timeRange.start).tz(timezone).local(true).startOf("day")
    let calcStart = localStart.add(1, "day")
    while (calcStart.valueOf() <= localEnd.valueOf()) {
      let d = dataSet.get(calcStart.valueOf())
      if (d) {
        let f = {...d}
        f.time = calcStart.valueOf()
        realData.push(f);
        calcStart = calcStart.add(1, "day");
        continue
      }
      realData.push({time: calcStart.valueOf()});
      calcStart = calcStart.add(1, "day");
    }
  }

  const formatValue = (value:number): string=>{
    return formatDefaultValueWithUnit(multFactor ? value * multFactor : value ,unit)

  }

  return <div>
    {graphData &&
      <ResponsiveContainer width="95%" height={200}>
        <BarChart barGap={"1%"} barCategoryGap="3%" className={"Graph"} data={realData}
                   margin={{top: 5, right: 30, left: 20, bottom: 5}} >
          <CartesianGrid strokeDasharray="3 3"/>
          <XAxis dataKey="time"
                 tickFormatter={(unixTime) => {
                   return (timezone?moment(unixTime).tz(timezone).local(false):moment(unixTime)).format('DD.MM')}
                 }/>
          <YAxis tickFormatter={value => formatValue(value)}/>
          <Tooltip formatter = {(value:string, name:string) => {
            return [formatValue(Number(value)), getValueNameOverrides(name)]
          }} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD')}/>
          <Legend formatter={(value, entry, index) =>
            <span className="text-color-class">{getValueNameOverrides(value)}</span>}/>
          {labels.map((l,index)=>{
            return <Bar fill={colors?colors[index]:getGraphColourByIndex(index)} key={index} type="monotone" dataKey={l}>){
              realData.map((entry, i) => {
                return <Cell key={i} fill={entry[l] >= 0 && colors ? colors[index]:getGraphColourByIndex(index)}/>
              })}</Bar>})}
        </BarChart>
      </ResponsiveContainer>
    }
  </div>

}
