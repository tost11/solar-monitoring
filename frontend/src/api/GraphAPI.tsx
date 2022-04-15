import {doRequest} from "./APIFunktions";

function getPathForType(type:string):string{
  if(type == "SELFMADE" || type == "SELFMADE_DEVICE" || type == "SELFMADE_CONSUMPTION" || type == "SELFMADE_INVERTER"){
    return "selfmade"
  }
  if(type == "GRID"){
    return "grid"
  }
  if( type == "SIMPLE" || type == "VERY_SIMPLE"){
    return "simple"
  }
  throw "Type "+type+" not implementd";
}

export interface GraphDataDTO{
  data:[],
  deviceIds?:number[]
}

export function getAllGraphData(systemId:number,type:string,from:number,to:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/"+getPathForType(type)+"/all?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticGraphData(systemId:number,type:string,from:number,to:number):Promise<[]>{
  return doRequest<[]>(window.location.origin+"/api/influx/"+getPathForType(type)+"/statistics?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function fetchLastFiveMinutes(systemId:number,type:string,duration:number):Promise<GraphDataDTO>{
  return doRequest<GraphDataDTO>(window.location.origin+"/api/influx/"+getPathForType(type)+"/latest?systemId="+systemId+"&duration="+duration,"GET")
}