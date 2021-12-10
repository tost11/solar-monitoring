import React, {useContext} from "react";
import {doRequest} from "./APIFunktions"
import {UserContext} from "../UserContext";

export interface Login{
  name:string;
  id:number;
  jwt:string;
}
export interface LoginDTO{
  name:string;
  password:string;
}


export function postLogin():(body:LoginDTO)=>Promise<Login>{
  return doRequest<Login>(window.location.href+"api/user/login","Post")
}

/*
export function generateLoginBody(name:string,password:string):LoginDTO{
  return{name,password};
}
*/