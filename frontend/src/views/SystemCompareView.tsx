import ContinuousUpdateWrapper, {ContinuousUpdateWrapperProps} from "../Component/ContinuousUpdateWrapper";
import {getPublicSystems, getSystems, SolarSystemListDTO} from "../api/SolarSystemAPI";
import React, {useEffect} from "react";
import {getAllCombinedGraphData} from "../api/GraphAPI";

export default function SystemCompareView() {

  useEffect(()=>{
    getAllCombinedGraphData(["646a7e0184a89e6046ec58d7","646a7e0284a89e6046ec58d8"],new Date().getTime()-100000,new Date().getTime()).then(res=>{
      console.log(res);
    });
  },[])

  return <div>
    {/*<ContinuousUpdateWrapper fetchTimout={1000 * 10} fetchFunction={getPublicSystems}/>*/}
  </div>
}
