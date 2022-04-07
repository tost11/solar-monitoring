import {doRequest} from "./APIFunktions";

export function getAllGraphData(systemId:number,from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/getAllData?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticGraphData(systemId:number,from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/Statistics?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function fetchLastFiveMinutes(systemId:number,duration:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/latest?systemId="+systemId+"&duration="+duration,"GET")
}