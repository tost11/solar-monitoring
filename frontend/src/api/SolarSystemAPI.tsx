import React from "react";
import {doRequest, doRequestNoBody} from "./APIFunktions"

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
  timezone: string
  managers:ManagerDTO[]
  publicMode: "NONE"|"ALL"|"PRODUCTION"
}

export interface RegisterSolarSystemDTO{
  name: string
  buildingDate?: Date
  type: string
  id: number
  token:string
  latitude:number
  longitude:number
  timezone:string
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
  batteryVoltage?: number
  isBatteryPercentage?: boolean
  maxSolarVoltage?: number
  inverterVoltage?: number
  type:string
  timezone: string
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
export interface addMangerDTO{
  id:number
  systemId:number
  role:string
}


export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")

}

export function getSystems():Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/all","GET")
}

export function getPublicSystems():Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/public/all","GET")
}

export function patchSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number,timezone:string|null,publicMode:string,id?:number):Promise<RegisterSolarSystemDTO> {
  let body = {id,name,buildingDate,type,isBatteryPercentage,inverterVoltage,batteryVoltage,maxSolarVoltage,timezone,publicMode}
  return doRequest(window.location.origin + "/api/system/edit", "POST", body)
}

export function createSystem(name:string,buildingDate:number,type: string,isBatteryPercentage:boolean,inverterVoltage:number,batteryVoltage:number,maxSolarVoltage:number,timezone:string|null,publicMode:string):Promise<RegisterSolarSystemDTO> {
  let body = {name, buildingDate, type, isBatteryPercentage, inverterVoltage, batteryVoltage, maxSolarVoltage, timezone,publicMode}
  return doRequest(window.location.origin + "/api/system/", "POST", body)
}
export function deleteSystem(systemId:number){
  return doRequestNoBody(window.location.origin+"/api/system/delete/"+systemId,"POST")
}
export function getManagers(systemId:number):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/allManager/"+systemId,"GET")
}

export function setManageUser(manager:addMangerDTO):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/addManageBy","POST",manager)
}
export function deleteMangerRelation(managerId:number,systemId:number):Promise<SolarSystemDTO>{
   return doRequest(window.location.origin+"/api/system/deleteManager/"+managerId+"/"+systemId,"POST")

}
export function createNewToken(systemId:number):Promise<NewTokenDTO>{
  return doRequest<NewTokenDTO>(window.location.origin+"/api/system/newToken/"+systemId,"GET")
}

