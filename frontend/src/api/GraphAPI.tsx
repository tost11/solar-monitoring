import {doRequest} from "./APIFunktions";

export interface DeviceIds{
  inputDCIds: number[]
  inputACIds: number[]
  outputDCIds: number[]
  outputACIds: number[]
  batteryIds: number[]
}

export interface TotalData{
  calcProducedKWH? :number
  producedKWH? :number
  calcProducedKWHPrice? :number
  producedKWHPrice? :number

  calcConsumedKWH? :number
  consumedKWH? :number
  calcConsumedKWHPrice? :number
  consumedKWHPrice? :number

  calcProducedKWHDay? :number
  producedKWHDay? :number
  calcProducedKWHPriceDay? :number
  producedKWHPriceDay? :number

  calcConsumedKWHDay? :number
  consumedKWHDay? :number
  calcConsumedKWHPriceDay? :number
  consumedKWHPriceDay? :number
}


export interface DeviceIdsWrapper{
  [key:string]: DeviceIds
}

export interface GraphDataDTO{
  data:[]
  devices: DeviceIdsWrapper
  totalData: TotalData
}

export interface GraphDataObject{
  data:any[]
  totalData: TotalData
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

export function fetchLastFiveMinutesCombined(systemIds:string[],duration:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/combined/latest?"+systemIds.map(e=>"SystemIds="+e).join("&")+"&duration="+duration,"GET")
}

export function getCombinedStatisticGraphData(systemIds:string[],from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/combined/statistics/all?"+systemIds.map(e=>"SystemIds="+e).join("&")+"&from="+from+"&to="+to,"GET")
}

export function getCombinedStatisticLastTwoDaysGraphData(systemIds:string[]):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/statistics/latest?"+systemIds.map(e=>"SystemIds="+e).join("&"),"GET")
}
