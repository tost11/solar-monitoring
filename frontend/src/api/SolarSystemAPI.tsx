import React from "react";
import {doRequest} from "./APIFunktions"

export interface SolarSystemDTO{
  name: string
  buildingDate?:Date
  creationDate:Date
  type: string
  id: number
  isBatteryPercentage:boolean
  inverterVoltage:number
  batteryVoltage:number
  maxSolarVoltage:number
  latitude?:number
  longitude?:number
  managers:ManagerDTO[]
}

export interface RegisterSolarSystemDTO{
  name: string
  buildingDate?: Date
  type: string
  id: number
  token:string
  latitude:number
  longitude:number
}

export interface SolarSystemListDTO{
  name: string
  type: string
  id: number
  role:string
}

export interface SolarSystemDashboardDTO{
  name:string
  buildingDate?:Date
  creationDate:Date
  type:string
  id:number
}

export interface ManagerDTO{
  id:number
  userName:string
  role:string
}

export interface NewTokenDTO{
  token: string
}

export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")

}

export function getSystems():Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/all","GET")
}

export function patchSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number,id?:number):Promise<RegisterSolarSystemDTO> {
  let body = {id,name,buildingDate,type,isBatteryPercentage,inverterVoltage,batteryVoltage,maxSolarVoltage}
  return doRequest(window.location.origin + "/api/system/patch", "POST", body)
}

export function createSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number):Promise<RegisterSolarSystemDTO>{
  let body = {name,buildingDate,type,isBatteryPercentage,inverterVoltage,batteryVoltage,maxSolarVoltage}
  return doRequest(window.location.origin+"/api/system/","POST",body)

  }
export function getManagers(systemId:number):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/allManager/"+systemId,"GET")
}
export function setManageUser(id:number,solarID:number,permission:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/addManageBy/"+id+"/"+solarID+"/"+permission,"POST")
}

export function createNewToken(systemId:number):Promise<NewTokenDTO>{
  return doRequest<NewTokenDTO>(window.location.origin+"/api/system/newToken/"+systemId,"GET")
}

