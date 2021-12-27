import React from "react";
import {doRequest} from "./APIFunktions"

export interface SolarSystemDTO{
  name:string
  creationDate:number
  type:string
}
export interface SolarSystemListDTO{
  name:string[]
}

export function getSystem():(body:SolarSystemDTO)=>Promise<SolarSystemDTO>{
  return doRequest(window.location.href+"api/system/","GET")


}
export function getSystems():(body:any)=>Promise<SolarSystemListDTO>{
  return doRequest(window.location.origin+"/api/system/all","GET")
}

export function createSystem():(body:SolarSystemDTO)=>Promise<SolarSystemDTO>{

  return doRequest(window.location.origin+"/api/system/","POST")

  }
