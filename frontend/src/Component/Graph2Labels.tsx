import React, {useContext, useEffect, useState} from "react";
import {SolarSystemDashboardDTO} from "../api/SolarSystemAPI";
import {getSolarData, GraphDTO} from "../api/GraphAPI";
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
  panelData?:GraphDTO[]
  systemInfo: SolarSystemDashboardDTO;
  timeRange: string;
}

export default function Graph({timeRange,systemInfo,onLoad,panelData}:GraphProps) {
  const [graphData,setGraphData] = useState<ChartData>()

  useEffect(() => {
  }, [timeRange])
  
const setData = (data:[],labels:[]) => {
  if (panelData) {
    setGraphData({
      labels: labels,
      datasets: [
        {
          label: panelData[0].label,
          data: data,
          borderColor: 'rgb(255, 99, 132)',
          backgroundColor: 'rgba(255, 99, 132, 0.5)'
        },
        {
          label: "DeviceTemperature",
          data: [2, 3, 4, 5, 6, 7, 8, 9, 0, 17, 45, 64, 64, 4, 6],
          borderColor: 'rgb(0, 99, 132)',
          backgroundColor: 'rgba(0, 99, 132,0,5)'
        }
      ]
    })
  }}

   const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {text: "This is a test"},
      scales: {
        xAxes: [{
          type: 'time',
          gridLines: {
            lineWidth: 2
          },
          time: {
            unit: "day",
            unitStepSize: 1000,
            displayFormats: {
              second: 'MMM DD',
              minute: 'MMM DD',
              hour: 'MMM DD',
              day: 'MMM DD',
              month: 'MMM DD',
              quarter: 'MMM DD',
              year: 'MMM DD',
            }
          }
        }]
      }
    }
   }



  return<div>
    {graphData?<Line options={options} data={graphData} />:<div>loading</div>}</div>
}

