import React, {useContext, useEffect, useState} from "react";
import * as Giraffe from '@influxdata/giraffe'
import {Config, fromFlux, LayerConfig, Plot} from "@influxdata/giraffe";
import {CsvDTO, getSolarCSV} from "../api/GraphAPI";


export interface GraphProps{
  csv:CsvDTO
}
export default function Graph({csv}:GraphProps){
  const [config,setConfig]=useState<Config>()
  const setData = (r:string) => {
    setConfig({
      fluxResponse: r,
      layers: [lineLayer],
    })
  }

  useEffect(()=> {
    console.log(csv)
    getSolarCSV(csv).then((r) => {
      setData(r.csv)
    })
  },[])

  const lineLayer:LayerConfig={
    type: "line",
    x: "_time",
    y: "_value",
  }
  const table = Giraffe.newTable(3)
    .addColumn('_time', 'dateTime:RFC3339', 'time', [1589838401244, 1589838461244, 1589838521244])
    .addColumn('_value', 'double', 'number', [2.58, 7.11, 4.79]);



  return<div>
    {config&&<div style={{
        width: "450px",
        height: "200px",
        margin: "40px",
      }}
    >
      <Plot config={config} />
    </div>}

  </div>
}
