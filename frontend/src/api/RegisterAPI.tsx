import React from "react";
import { useState } from "react";
import { Login } from "../UserContext";
import {doRequest} from "./APIFunktions"


export interface RegisterDTO{
  name:string;
  password:string;
  controlPassword:string;
}

export function postRegister():(body:RegisterDTO)=>Promise<Login>{
  return  doRequest<Login>(window.location.href+"api/user/register","Get")

}
