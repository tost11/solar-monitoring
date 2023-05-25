import React from "react";
import TimeAndDateSelector, {generateTimeDuration} from "../Component/time/TimeAndDateSelector";
import moment from "moment";


export default function TestView() {

  console.log(moment())

  let toUse = {
    time: generateTimeDuration("1w",moment()),
    autoUpdate: false
  }

  let toUse2 = {
    time: generateTimeDuration("1w",moment()),
    autoUpdate: false
  }

  console.log("to use: ",toUse)
  console.log("to use2: ",toUse2)


  return <div>
    <h2>Test</h2>


    <TimeAndDateSelector minDate={moment()} onlyDate={false} onChange={(time,nowButton)=>{console.log("changed:",time)}}
                         timeRange={toUse} timezone={"America/Los_Angeles"} timeRanges={["1w","2w","1M","2M","6M","1y"]}/>

    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}
  </div>
}
