import React, {useContext, useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {getSolarCSV} from "../api/GraphAPI";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import {ChartData} from "chart.js";
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);


export interface GraphProps{
  onLoad?:(a:boolean)=>void
  systemInfo: SolarSystemDashboardDTO;
  timeRange: string;
}

export default function Graph({timeRange,systemInfo,onLoad}:GraphProps) {
  const [graphData,setGraphData] = useState<ChartData>()
  useEffect(() => {
    getSolarCSV({systemId: systemInfo.id, field: "DeviceTemperature", from: "-" + timeRange, to: "now()"}).then((r) => {
      setData(r.data,r.time)
      if (onLoad) {
        onLoad(false)
      }
    })
  }, [timeRange])
const setData = (data:[],lables:[]) => {
  setGraphData({
    labels:lables,
    datasets: [
    {
      label: 'Dataset 1',
      data:data,
      borderColor: 'rgb(255, 99, 132)',
      backgroundColor: 'rgba(255, 99, 132, 0.5)'
    }
    ]
})}

   const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Chart.js Line Chart',
      },
    },
   }



  return<div>
    {graphData?<Line options={options} data={graphData} />:<div>loading</div>}</div>
}

