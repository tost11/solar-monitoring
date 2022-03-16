import {doRequest} from "./APIFunktions";

export interface GraphDTO{
  csv:string;
}
export interface CsvDTO{
  systemId:number
  field:string
  from:string
  to:string
}

export function getSolarCSV(csvDTO:CsvDTO):Promise<GraphDTO>{
  console.log(csvDTO)
  return doRequest<GraphDTO>(window.location.origin+"/api/influx","POST",csvDTO)
}
