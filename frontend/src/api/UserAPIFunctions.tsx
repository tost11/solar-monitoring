import React from "react";
import {doRequest} from "./APIFunktions"
import {Login} from "../context/UserContext";

export interface LoginDTO{
  name:string;
  password:string;
}
export interface UserDTO{
  id:number,
  name:string,
  numAllowedSystems:number,
  admin:boolean,
}

export interface GenericDataDTO{
  id:number,
  name:string
}

export function postLogin(name:string,password:string):Promise<Login>{
  let body={name,password};
  return doRequest<Login>(window.location.origin+"/api/user/login","Post",body)
}

export function postRegister(name:string|null,password:string|null): Promise<Login> {
  let body = {name, password};
  return doRequest<Login>(window.location.origin + "/api/user/register", "POST", body)
}

export function findUsers(name:string):Promise<GenericDataDTO[]>{
 return doRequest<GenericDataDTO[]>(window.location.origin+"/api/user/findUser/"+name,"GET")
}

export function findUsersForSettings(name:string):Promise<UserDTO[]>{
  return doRequest<UserDTO[]>(window.location.origin+"/api/user/admin/findUser/"+name,"GET")
}

export function patchUser(body:UserDTO):Promise<UserDTO>{
  return doRequest(window.location.origin+"/api/user/edit", "POST",body)
}
