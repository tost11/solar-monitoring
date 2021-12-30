import React from "react";
import {doRequest} from "./APIFunktions"
import {Login} from "../context/UserContext";

export interface LoginDTO{
  name:string;
  password:string;
}

export function postLogin(name:string,password:string):Promise<Login>{
  return doRequest<Login>(window.location.origin+"/api/user/login","Post",{name,password})
}

export function postRegister(name:string,password:string): Promise<Login> {
  return doRequest<Login>(window.location.origin + "/api/user/register", "POST",{name,password})

}
