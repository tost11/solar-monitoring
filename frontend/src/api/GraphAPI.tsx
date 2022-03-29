import {doRequest} from "./APIFunktions";

export interface GraphDTO{

  data:[],
}
export interface CsvDTO{
  systemId:number
  field:string
  from:string
  to:string
}

export function getAllGraphData(systemId:number,from:number):Promise<GraphDTO>{
  return doRequest<GraphDTO>(window.location.origin+"/api/influx/getAllData?systemId="+systemId+"&from="+from,"GET")
}
