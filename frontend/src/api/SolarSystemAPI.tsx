import React from "react";
import {doRequest} from "./APIFunktions"

export interface SolarSystemDTO{
  name:string
  creationDate:number
  type:string
  grafanaUid:string

}
export interface SolarSystemListDTO{
  name:string
  type:string
  id:number

}

export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")


}
export function getSystems():Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/all","GET")
}


export function createSystem(name:string,creationDate:number,type: string):Promise<SolarSystemDTO>{
  let body = {name,creationDate,type}
  return doRequest(window.location.origin+"/api/system/","POST",body)

  }
