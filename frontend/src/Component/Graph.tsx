import React, {useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {getSolarCSV} from "../api/GraphAPI";
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
    getSolarCSV({systemId: systemInfo.id, field: "DeviceTemperature", from: "-" + timeRange, to: "now()"}).then((r) => {
      var data = []
      for(var i=0;i<r.data.length;i++){
        data.push({
          value: r.data[i],
          time: moment(r.time[i]).toDate().getTime()
        })
      }
      console.log(data)
      setGraphData({data})
    })
  }, [timeRange])

  const data = [{time:1648491912969, value: 12,value2: 12},{time:1648491913969, value: 15,value2: 12},{time:1648492008405, value: 20,value2: 12}]

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
      </LineChart>
    }
  </div>

}

