import React from "react";
import {doRequest, doRequestNoBody} from "./APIFunktions"

export enum SolarSystemType {
  SELFMADE= "SELFMADE",
  SIMPLE = "SIMPLE",
  VERY_SIMPLE = "VERY_SIMPLE",
  GRID = "GRID",
  GRID_BATTERY = "GRID_BATTERY"
}

export interface BooleanStatus {
  name: string,
  lastSet: Date,
  value: boolean
}

export interface AllStatus {
  booleans: BooleanStatus[]
}


export enum SolarSystemPublicMode {
  NONE = "NONE",
  ALL = "ALL",
  PRODUCTION = "PRODUCTION"
}

export interface SolarSystemDTO{
  name: string
  buildingDate?:Date
  creationDate:Date
  type: SolarSystemType
  id: number
  isBatteryPercentage:boolean
  hasACInput:boolean,
  hasACOutput:boolean,
  hasDCOutput:boolean,
  voltageAC:number
  batteryVoltage:number
  maxSolarVoltage:number
  latitude?:number
  longitude?:number
  timezone: string
  managers:ManagerDTO[]
  publicMode: SolarSystemPublicMode,
  status: AllStatus
}

export interface CreateSolarSystemDTO{
  name: string
  buildingDate?:Date
  type: SolarSystemType
  isBatteryPercentage:boolean
  hasACInput?:boolean,
  hasACOutput?:boolean,
  hasDCOutput?:boolean,
  voltageAC?:number
  batteryVoltage?:number
  maxSolarVoltage?:number
  latitude?:number
  longitude?:number
  timezone: string
  publicMode: SolarSystemPublicMode
}


export interface PatchSolarSystemDTO extends CreateSolarSystemDTO{
  id: number
}


export interface RegisterSolarSystemResponseDTO {
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

export function patchSystem(dto:PatchSolarSystemDTO):Promise<RegisterSolarSystemResponseDTO> {
  return doRequest(window.location.origin + "/api/system/edit", "POST", dto)
}

export function createSystem(dto:CreateSolarSystemDTO):Promise<RegisterSolarSystemResponseDTO> {
  return doRequest(window.location.origin + "/api/system", "POST", dto)
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

export function updateStatistics(systemId:number):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/statistics/"+systemId,"GET")
}

export function addBooleanStatus(systemId:number,name: string):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"PUT")
}

export function deleteBooleanStatus(systemId:number,name?: string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"DELETE")
}

export function setBooleanStatus(systemId:number,name: string,value:boolean):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name+"&value="+value,"POST")
}
