import React, {useContext, useEffect, useState} from "react";
import * as Giraffe from '@influxdata/giraffe'
import {
  Config,
  HoverTimeProvider,
  LayerConfig,
  NINETEEN_EIGHTY_FOUR,
  Plot, timeFormatter
} from "@influxdata/giraffe";
import { getSolarCSV} from "../api/GraphAPI";
import {SolarSystemDashboardDTO} from "../api/SolarSystemAPI";


export interface GraphProps{
  onLoad?:(a:boolean)=>void

  systemInfo: SolarSystemDashboardDTO;
  timeRange: string;
}
export default function Graph({timeRange,systemInfo,onLoad}:GraphProps) {
  const [config, setConfig] = useState<Config>()
  const setData = (r: string) => {
    setConfig({
      fluxResponse: r,
      layers: [lineLayer,lineLayer],
      valueFormatters: {
        _time: timeFormatter({
          locale:"default",
           format: "HH:mm",
    })
      },
    })
  }

  useEffect(() => {
    getSolarCSV({systemId: systemInfo.id, field: "DeviceTemperature", from: "-" + timeRange, to: "now()"}).then((r) => {
      setData(r.csv)
      if (onLoad) {
        onLoad(false)
      }
    })
  }, [timeRange])


  const lineLayer: LayerConfig = {
    type: "band",
    x: "_time",
    y: "_value",
    fill: [],
    mainColumnName: "Power"
  }
  const table = Giraffe.newTable(3)
    .addColumn('_time', 'dateTime:RFC3339', 'time', [1589838401244, 1589838461244, 1589838521244])
    .addColumn('_value', 'double', 'number', [2.58, 7.11, 4.79]);


  return <div>

    {config && <HoverTimeProvider>
      <div style={{
        width: "450px",
        height: "200px",
      }}
      >
        <Plot config={config}/>
      </div>
    </HoverTimeProvider>}


  </div>
}

