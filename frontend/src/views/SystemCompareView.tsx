import ContinuousUpdateWrapper, {ContinuousUpdateWrapperProps} from "../Component/ContinuousUpdateWrapper";
import {getPublicSystems, getSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import React from "react";

export default function SystemCompareView() {

  const updateData = () => {
    console.log(res)
  }

  return <div>
    <ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>
  </div>
}
