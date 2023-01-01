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
  timer?:any,
  devices: DeviceIdsWrapper
}

export function getAllGraphData(systemId:number,from:number,to:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/all?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticGraphData(systemId:number,from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/statistics?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function fetchLastFiveMinutes(systemId:number,duration:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/latest?systemId="+systemId+"&duration="+duration,"GET")
}
