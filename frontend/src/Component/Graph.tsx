import React, {useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {getAllGraphData} from "../api/GraphAPI";
import {CartesianGrid, Legend, Line, LineChart, Tooltip, XAxis, YAxis} from "recharts";
import moment from "moment";
import {convertToDuration} from "./TimeSelector";

export interface GraphProps{
  onLoad?:(a:boolean)=>void
  systemInfo: SolarSystemDashboardDTO;
  timeRange: string;
}

export default function Graph({timeRange,systemInfo,onLoad}:GraphProps) {
  const [graphData,setGraphData] = useState()
  useEffect(() => {
    getAllGraphData( systemInfo.id,convertToDuration(timeRange).start.getTime()).then((r) => {
      var data = []
      for(var i=0;i<r.data.length;i++){
        data.push({
          value: r.data[i],
          value2: r.data[i]+5,
          time: moment(r.time[i]).toDate().getTime()
        })
      }
      console.log(data)
      setGraphData({data})
    })
  }, [timeRange])

  const dur = convertToDuration(timeRange);


  return <div>
    {graphData &&
      <LineChart width={730} height={250} data={graphData.data}
                 margin={{top: 5, right: 30, left: 20, bottom: 5}}>
        <CartesianGrid strokeDasharray="3 3"/>
        <XAxis dataKey="time"
               domain={[dur.start.getTime(), dur.end.getTime()]}
               type='number'
               tickFormatter={(unixTime) => moment(unixTime).format('HH:mm')}/>
        <YAxis dataKey='value' name='Value'/>
        <Tooltip/>
        <Legend/>
        <Line type="monotone" dataKey="value" stroke="#8884d8"/>
        <Line type="monotone" dataKey="value2" stroke="#FFFFFF"/>
      </LineChart>
    }
  </div>

}

