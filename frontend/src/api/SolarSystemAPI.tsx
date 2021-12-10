import React from "react";
import {doRequest} from "./APIFunktions"
import {SolarSystem} from "../UserContext"

export interface SolarSystemDTO{
  name:string
}

export function getSystem():(body:SolarSystemDTO)=>Promise<SolarSystem>{
  return doRequest(window.location.href+"api/system/","GET")


}

export function createSystem():(body:SolarSystemDTO)=>Promise<SolarSystem>{

  return doRequest(window.location.href+"api/system/","Post")

  }
