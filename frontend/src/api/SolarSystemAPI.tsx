import React from "react";
import {doRequest} from "./APIFunktions"

export interface SolarSystemDTO{
  name: string
  buildingDate:number
  creationDate:number
  type: string
  id: number
  isBatteryPercentage:boolean
  inverterVoltage:number
  batteryVoltage:number
  maxSolarVoltage:number


}
export interface RegisterSolarSystemDTO{
  name: string
  buildingDate: number
  type: string
  id: number
  token:string




}
export interface SolarSystemListDTO{
  name: string
  type: string
  id: number
}

export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")

}

export function getSystems():Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/all","GET")
}

export function patchSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number):Promise<RegisterSolarSystemDTO> {
  let body = {name,buildingDate,type,isBatteryPercentage,inverterVoltage,batteryVoltage,maxSolarVoltage}
  return doRequest(window.location.origin + "/api/system/patch", "POST", body)
}

export function createSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number):Promise<RegisterSolarSystemDTO>{
  let body = {name,buildingDate,type,isBatteryPercentage,inverterVoltage,batteryVoltage,maxSolarVoltage}
  return doRequest(window.location.origin+"/api/system/","POST",body)

  }
