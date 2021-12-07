import React from "react";
import {getRequest,postRequest,delRequest} from "./api/APIFunktions"


export interface SolarSystem{
  name:string;
}

export async function getSystem():Promise<SolarSystem>{
  let path= window.location.href+"api/system/1"
  let response = await getRequest(path)
  return await response.json();

}

export async function createSystem(){
  let body= JSON.stringify(name)
  let path= window.location.href+"api/system/1"
  let response = await getRequest(path)
  return await response.json();

  }
