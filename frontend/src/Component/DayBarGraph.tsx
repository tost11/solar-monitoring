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

export default function DayBarGraph({negativeColours,colors,timezone,timeRange,graphData,labels,unit,multFactor}:BarGraphProps) {

  //console.log("Graph data day: ",graphData)
  //console.log("Graph data timerange: ",timeRange)

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
          <YAxis tickFormatter={value => formatDefaultValueWithUnit(multFactor ? value*multFactor : value, unit)}/>
          <Tooltip formatter={(value: number) => formatDefaultValueWithUnit(multFactor ? value*multFactor : value, unit)} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD')}/>
          <Legend />
          {labels.map((l,index)=>{
            return <Bar fill={negativeColours ? undefined : getColourAtIndex(usedColors,index)} key={index} type="monotone" dataKey={l}>){
              realData.map((entry, i) => {
                return <Cell key={i} fill={entry[l] >= 0
                  ? getColourAtIndex(usedColors,index) // green
                  : getColourAtIndex(usedNegativeColors,index)}/>
              })}</Bar>})}

        </BarChart>
      </ResponsiveContainer>
    }
  </div>

}
