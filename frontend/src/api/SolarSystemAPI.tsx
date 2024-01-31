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

export interface NamingsDTO {
  devices: {[key: string]: string}
  batteries: {[key: string]: string},
  inputsAC: {[key: string]: string},
  inputsDC: {[key: string]: string},
  outputsAC: {[key: string]: string}
  outputsDC: {[key: string]: string}
}

/*export interface NamingsMap{
  devices: Map<number,string>,
  batteries: Map<number,string>,
  inputs: Map<number,string>,
  outputs: Map<number,string>
}*/

export interface ViewData{
  showAmpere:boolean
  isBatteryPercentage?:boolean
  hasACInput?:boolean
  hasACOutput?:boolean
  hasDCOutput?:boolean
  hasTemperature?:boolean
  productionForTotalPricing?:boolean
  voltageAC?:number
  batteryVoltage?:number
  maxSolarVoltage?:number
  hideTotalConsumption?:boolean
  totalPricingPublicOverride?:boolean
}

export interface SolarSystemDTO{
  name: string,
  shortener: string,
  viewName: string,
  buildingDate?:Date,
  creationDate:Date,
  type: SolarSystemType,
  id: string,
  latitude?:number,
  longitude?:number,
  electricityPrice?:number,
  timezone: string,
  managers:ManagerDTO[],
  publicMode: SolarSystemPublicMode,
  status: AllStatus,
  publicFlagOnlyProduction: boolean,
  viewData: ViewData,
  namings: NamingsDTO
}

export interface CreateSolarSystemDTO{
  name: string,
  shortener?:string,
  buildingDate?:Date,
  type: SolarSystemType,
  latitude?:number,
  longitude?:number,
  electricityPrice?: number,
  timezone: string,
  publicMode: SolarSystemPublicMode,
  viewData: ViewData,
  namings: NamingsDTO
}


export interface PatchSolarSystemDTO extends CreateSolarSystemDTO{
  id: string
}

export interface RegisterSolarSystemResponseDTO{
  id: string,
  name: string,
  shortener:string,
  viewName: string,
  buildingDate?: Date,
  type: string,
  string: number,
  token:string,
  latitude:number,
  longitude:number,
  timezone:string
}

export interface CurrentValuesDTO{
  inputWatt?:number
  batteryVoltage?: number
}

export interface SolarSystemListDTO{
  name: string
  type: string
  id: string
  role:string
  shortener: string
  currentValues?:CurrentValuesDTO
}

export interface ManagerDTO{
  id:string,
  userName:string,
  role:string
}

export interface NewTokenDTO{
  token: string
}
export interface addMangerDTO{
  id:string,
  systemId:string,
  role:string
}

export interface MultSolarSystemDTO{
  name: string,
  type: SolarSystemType,
  id: string,
  publicMode: SolarSystemPublicMode,
  viewName: string
}


export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")
}

export function getSystems(isPublic:boolean):Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/all?public="+isPublic,"GET")
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
export function deleteSystem(systemId:string){
  return doRequestNoBody(window.location.origin+"/api/system/delete/"+systemId,"POST")
}
export function getManagers(systemId:string):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/allManager/"+systemId,"GET")
}

export function setManageUser(manager:addMangerDTO):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/addManageBy","POST",manager)
}
export function deleteMangerRelation(managerId:string,systemId:string):Promise<SolarSystemDTO>{
   return doRequest(window.location.origin+"/api/system/deleteManager/"+managerId+"/"+systemId,"POST")
}

export function createNewToken(systemId:string):Promise<NewTokenDTO>{
  return doRequest<NewTokenDTO>(window.location.origin+"/api/system/newToken/"+systemId,"GET")
}

export function updateStatistics(systemId:string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/statistics/"+systemId,"GET")
}

export function addBooleanStatus(systemId:string,name: string):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"PUT")
}

export function deleteBooleanStatus(systemId:string,name?: string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"DELETE")
}

export function setBooleanStatus(systemId:string,name: string,value:boolean):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name+"&value="+value,"POST")
}

export function getMultSystems(ids:string[]):Promise<MultSolarSystemDTO[]>{
  return doRequest<MultSolarSystemDTO[]>(window.location.origin+"/api/system/mult?"+ids.map(s=>"systemIds="+s).join("&"),"GET")
}
