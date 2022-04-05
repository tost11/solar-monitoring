import {doRequest} from "./APIFunktions";

export function getAllGraphData(systemId:number,from:number,to:number):Promise<Object[]>{
  return doRequest<Object[]>(window.location.origin+"/api/influx/getAllData?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}

export function getStatisticGraphData(systemId:number,from:number,to:number):Promise<Object[]>{
  return doRequest<Object[]>(window.location.origin+"/api/influx/Statistics?systemId="+systemId+"&from="+from+"&to="+to,"GET")
}
