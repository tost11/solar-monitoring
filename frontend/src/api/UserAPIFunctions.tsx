import React from "react";
import {doRequest} from "./APIFunktions"
import {Login} from "../context/UserContext";

export interface LoginDTO{
  name:string;
  password:string;
}
export interface UserDTO{
  name:string,
  numbAllowedSystems:number,
  admin:boolean,
}

export function postLogin(name:string,password:string):Promise<Login>{
  let body={name,password};
  return doRequest<Login>(window.location.origin+"/api/user/login","Post",body)
}

export function postRegister(name:string|null,password:string|null): Promise<Login> {
  let body = {name, password};
  return doRequest<Login>(window.location.origin + "/api/user/register", "POST", body)
}

export function getAllUser():Promise<UserDTO[]>{
 return doRequest<UserDTO[]>(window.location.origin+"/api/user/getAllUser","GET")
}

export function patchUser(body:UserDTO):Promise<UserDTO>{
  return doRequest(window.location.origin + "/api/user/patch/"+body.name, "POST",body)
}
