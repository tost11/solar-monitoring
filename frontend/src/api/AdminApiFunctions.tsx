import {doRequest, doRequestNoBody} from "./APIFunktions";

export interface ConfigDTO{
  isRegistrationEnabled: boolean;
}

export function fetchApplicationConfig():Promise<ConfigDTO>{
  return doRequest<ConfigDTO>(window.location.origin+"/api/admin/config","GET")
}

export async function fetchSetRegistration(enabled:boolean){
  return doRequestNoBody(window.location.origin+"/api/admin/config/registration?enabled="+enabled,"POST")
}