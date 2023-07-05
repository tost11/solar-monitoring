import React, {useState} from "react";
import TimeAndDateSelector, {generateTimeDuration} from "../Component/time/TimeAndDateSelector";
import moment from "moment";
import DayBarGraph from "../Component/DayBarGraph";
import LineGraph from "../Component/LineGraph";


export default function TestView() {

  console.log(moment())

  let toUse = {
    time: generateTimeDuration("1w",moment()),
    autoUpdate: false
  }

  let toUse2 = {
    time: generateTimeDuration("4h",moment()),
    autoUpdate: false
  }

  var [timeRange,setTimeRange] = useState(toUse)

  console.log("to use: ",toUse)
  console.log("to use2: ",toUse2)



  var timeZone = "America/Los_Angeles";

  return <div>
    <h2>Test</h2>


    <TimeAndDateSelector minDate={moment()} onlyDate={false} onChange={(time,nowButton)=>{
      console.log("changed:",time)
      setTimeRange(time)
    }}
     timeRange={timeRange} timezone={timeZone} timeRanges={["4h","1w","2w","1M","2M","6M","1y"]}/>

    <DayBarGraph
      graphData={{data:[{time:1684454400000,test:"100"}]}}
      timeRange={timeRange.time}
      timezone={timeZone}
      labels={["test"]}
    />

    <LineGraph
      graphData={{data:[]}}
      timeRange={timeRange.time}
      timezone={timeZone}
      labels={["test"]}
    />

    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}
  </div>
}
