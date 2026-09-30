import React, { useId } from "react";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
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
  fillFirstLine?: boolean
}


export default function LineGraph({defaultDuration,valueNameOverrides,timezone,timeRange,graphData,unit,labels,min,max,legendOverrideValue,deviceColours,defaultDurations,calculatedFields,fillFirstLine}:GraphProps) {

  const gradientId = useId().replace(/:/g, "");
  const firstColor = deviceColours ? deviceColours[0] : getGraphColourByIndex(0);

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

  const graphStep = (timeRange.duration / 1000 / augmentedData.length);

  return <div>
    {graphData &&
      <ResponsiveContainer width="95%" height={200}>
        <ComposedChart className={"Graph"} data={augmentedData}
                       margin={{top: 5, right: 30, left: 20, bottom: 5}}>
          {fillFirstLine && labels.length > 0 && (
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={firstColor} stopOpacity={0.3} />
                <stop offset="100%" stopColor={firstColor} stopOpacity={0.03} />
              </linearGradient>
            </defs>
          )}
          <CartesianGrid strokeDasharray="3 3"/>
          <XAxis dataKey="time"
                 domain={[timeRange.start.valueOf(), timeRange.end.valueOf()]}
                 type='number'
                 tickFormatter={(unixTime) => (timezone?moment(unixTime).tz(timezone):moment(unixTime)).format('HH:mm')}/>
          <YAxis
              tickFormatter={value => formatDefaultValueWithUnit(value,unit)}
              domain={[min != undefined ? min : 'dataMin' , max != undefined ? max : 'dataMax' ]}
          />
          <Tooltip formatter={(value:any, name:string) => {
            if (value === undefined || value === null) return ['', ''];
            return [formatDefaultValueWithUnit(Number(value),unit), getValueNameOverrides(name)]
          }} labelFormatter={(unixTime) => moment(unixTime).format('yyyy-MM-DD HH:mm')}/>
          {legendOverrideValue ?
            <Legend content={() => <div>{legendOverrideValue}</div>}/>:
            <Legend formatter={(value, _entry, _index) => <span>{getValueNameOverrides(value)}</span>}/>
          }
          {labels.map((l, index) => {
            let durToUse = undefined;
            if(defaultDurations && defaultDurations[index]){
              durToUse = defaultDurations[index];
            }
            if(durToUse == undefined && defaultDuration){
              durToUse = defaultDuration;
            }
            let calcUse = (durToUse ? (durToUse) / 2 : 30 / 2) * 1.2
            const stroke = deviceColours ? deviceColours[index] : getGraphColourByIndex(index);
            const isFirst = fillFirstLine && index === 0;
            return isFirst ? (
              <Area key={index} connectNulls={graphStep < calcUse} dot={false} type="monotone"
                    dataKey={l} stroke={stroke} strokeWidth={2}
                    fill={`url(#${gradientId})`} name={l}/>
            ) : (
              <Line key={index} connectNulls={graphStep < calcUse} dot={false} type="monotone"
                    dataKey={l} stroke={stroke}/>
            );
          })}
        </ComposedChart>
      </ResponsiveContainer>
    }
  </div>

}
