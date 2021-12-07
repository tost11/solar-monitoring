import React from "react";
import { useState } from "react";
import {getRequest,postRequest,delRequest} from "./api/APIFunktions"


export interface SolarSystem{
  name:string;
}

export function getSystem(setSystems:(sols:SolarSystem[])=>void){
 // let body= JSON.stringify(name)
  localStorage.setItem("jwt","eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FhcyIsImV4cCI6MzY1ODkzODk5NDM5OTM0NSwiaWF0IjoxNjM4ODY1ODY3fQ.6UeCoLQA1XdiyycCpuIUYYKzxFWi-ToYctkjJc7_e6o")
  let path= window.location.href+"api/system/1"
  //let path= "http://localhost:8080/api/system/1"
  getRequest(path,(response:Response)=>{
    response.json().then((systems:SolarSystem[])=>{
      setSystems(systems)
    })
  })
}

export function createSystem(){
    alert("hi")

  }
