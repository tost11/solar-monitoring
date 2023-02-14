import React, {useEffect, useRef, useState} from "react";
import moment from "moment";

export interface ContinuousUpdateWrapperProps{
  fetchTimout: number,
  fullReloadTimeout?: number,
  updateCallback: () => Promise<boolean>,
  fullReloadCallback?: () => Promise<boolean>,
  active: boolean
}

export default function ContinuousUpdateWrapper({fetchTimout,updateCallback,active,fullReloadTimeout,fullReloadCallback}:ContinuousUpdateWrapperProps) {

  const lastSuccessfulCall = useRef(moment())

  const timer = useRef<NodeJS.Timeout>()

  const timoutCallback = async () => {
    timer.current = setTimeout(timoutCallback, fetchTimout)
    //console.log("new timeout started: ", timer)

    var last = lastSuccessfulCall.current

    let isOk: boolean;
    if (fullReloadTimeout) {
      let now = moment();
      var diff = now.diff(last)

      if (diff > fullReloadTimeout && fullReloadCallback) {
        isOk = await fullReloadCallback()
      } else {
        isOk = await updateCallback()
      }
    } else {
      isOk = await updateCallback()
    }

    if (isOk) {
      lastSuccessfulCall.current = moment()
    }
  }

  const internalClearTimeout=()=>{
    if(timer.current != undefined){
      //console.log("Clearing timer (may be old)",timer.current)
      clearTimeout(timer.current)
      timer.current = undefined
    }
  }

  const startNewTimeout= () => {
    //console.log("timeout started: ",timer)
    timer.current = setTimeout(timoutCallback, fetchTimout)
    lastSuccessfulCall.current = moment()
  }

  useEffect(()=>{
    startNewTimeout();

    return internalClearTimeout;
  }, [])

  useEffect(()=>{
    //console.log("Active changed")
    if(active){
      if(timer.current == undefined){
        startNewTimeout();
      }
    }else{
      if(timer.current != undefined){
        internalClearTimeout();
      }
    }
  },[active])

  return<>

  </>
}
