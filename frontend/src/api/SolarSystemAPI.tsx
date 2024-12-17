import React from "react";
import {doRequest, doRequestNoBody} from "./APIFunktions"
import moment from "moment";
import {TagDTO} from "./UserAPIFunctions";

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
  defaultDelay?:number
}

export interface SolarSystemDTO{
  name: string,
  shortener: string,
  viewName: string,
  buildingDate?:moment,
  creationDate:moment,
  type: SolarSystemType,
  id: string,
  latitude?:number,
  longitude?:number,
  electricityPrice?:number,
  deyeSunSerialNumbers?:string,
  timezone: string,
  managers:ManagerDTO[],
  publicMode: SolarSystemPublicMode,
  status: AllStatus,
  publicFlagOnlyProduction: boolean,
  viewData: ViewData,
  namings: NamingsDTO,
  calculateCombinedValuesAfterwards?: boolean,
  tags: TagDTO[],
}

export interface CreateSolarSystemDTO{
  name: string,
  shortener?:string,
  buildingDate?:moment,
  type: SolarSystemType,
  latitude?:number,
  longitude?:number,
  electricityPrice?: number,
  timezone: string,
  publicMode: SolarSystemPublicMode,
  viewData: ViewData,
  namings: NamingsDTO,
  deyeSunSerialNumbers?:string,
  calculateCombinedValuesAfterwards?:boolean
}


export interface PatchSolarSystemDTO extends CreateSolarSystemDTO{
  id: string
}

export interface RegisterSolarSystemResponseDTO{
  id: string,
  name: string,
  shortener:string,
  viewName: string,
  buildingDate?: moment,
  type: string,
  string: number,
  token:string,
  latitude:number,
  longitude:number,
  timezone:string,
  deyeSunSerialNumbers?:string,
  calculateCombinedValuesAfterwards?: boolean
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

export interface TagSolarSystemDTO{
  tag: TagDTO
  systems: SolarSystemListDTO[]
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
  id:string
  systemId:string
  role:string
}

export interface MultSolarSystemDTO{
  name: string
  type: SolarSystemType
  id: string
  publicMode: SolarSystemPublicMode
  defaultDuration?: number
  viewName: string
}

export interface SolarSystemSearchParams{
  public?: boolean,
  name?: string,
  tags?: string[],
  type?: SolarSystemType
}

export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")
}

export function getSystemInfo(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/public/"+id,"GET")
}

export function searchSystems(search:SolarSystemSearchParams):Promise<SolarSystemListDTO[]>{
  return doRequest<SolarSystemListDTO[]>(window.location.origin+"/api/system/search","POST",search)
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

export function getMultSystems(ids:string[],publicCall?:boolean):Promise<MultSolarSystemDTO[]>{
  return doRequest<MultSolarSystemDTO[]>(window.location.origin+"/api/system/"+(publicCall?"public/":"")+"mult?"+ids.map(s=>"systemIds="+s).join("&"),"GET")
}

export function getSystemsByTag():Promise<TagSolarSystemDTO[]>{
  return doRequest<TagSolarSystemDTO[]>(window.location.origin+"/api/tags/systems","GET")
}

export function findTagsById(ids:String[]):Promise<TagDTO[]>{
  if(!ids || ids.length == 0) {
    return new Promise((resolve, reject) => resolve([]));
  }
  const idString = ids.reduce(function (pre, next) {
    return pre + ',' + next;
  });
  return doRequest<TagDTO[]>(window.location.origin+"/api/tags/byIds?ids="+idString,"GET")
}
