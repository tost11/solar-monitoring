import React from "react";
import { useState } from "react";
import {getRequest,postRequest,delRequest} from "./api/APIFunktions"

export interface Login{
  name:string;
  id:number;
  jwt:string;
}

export async function postLogin(name:string,password:string):Promise<Login>{

  let body= JSON.stringify({name,password})
  localStorage.removeItem("jwt")

  let path= window.location.href+"api/user/login"

  let response = await postRequest(path,body)
  console.log("nein")
  return await response.json();
}
