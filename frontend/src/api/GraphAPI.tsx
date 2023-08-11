import {doRequest} from "./APIFunktions";

export interface DeviceIds{
  inputDCIds: number[],
  inputACIds: number[],
  outputDCIds: number[],
  outputACIds: number[],
  batteryIds: number[]
}

export interface DeviceIdsWrapper{
  [key:string]: DeviceIds
}

export interface GraphDataDTO{
  data:[],
  devices: DeviceIdsWrapper
}

export interface GraphDataObject{
  data:any[]
}

export interface DeviceGraphDataObject extends GraphDataObject{
  devices: DeviceIdsWrapper
}

export function getAllGraphData(systemId:string,from:number,to:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/all?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticGraphData(systemId:string,from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/statistics/all?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticLastTwoDaysGraphData(systemId:string):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/statistics/latest?systemId="+systemId,"GET")
}

export function fetchLastFiveMinutes(systemId:string,duration:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/latest?systemId="+systemId+"&duration="+duration,"GET")
}

export function getAllCombinedGraphData(systemIds:string[],from:number,to:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/combined/all?"+systemIds.map(e=>"SystemIds="+e).join("&")+"&from="+from+"&to="+to,"GET")
}
