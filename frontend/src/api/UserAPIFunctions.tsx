import React from "react";
import {doRequest} from "./APIFunktions"
import {Login} from "../context/UserContext";

export interface LoginDTO{
  name:string;
  password:string;
}
export interface AdminDTO{
  id:number,
  name:string,
  isAdmin:boolean
}

export function postLogin(name:string,password:string):Promise<Login>{
  let body={name,password};
  return doRequest<Login>(window.location.origin+"/api/user/login","Post",body)
}

export function postRegister(name:string|null,password:string|null): Promise<Login> {
  let body = {name, password};
  return doRequest<Login>(window.location.origin + "/api/user/register", "POST", body)
}

export function isUserAdmin():Promise<AdminDTO>{
 return doRequest<AdminDTO>(window.location.origin+"/api/user/isUser/Admin","GET")
}

export function makeUserToAdmin(id:number){
  return doRequest(window.location.origin + "/api/user/toAdmin"+id, "POST")
}
