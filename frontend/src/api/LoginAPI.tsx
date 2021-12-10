import React, {useContext} from "react";
import {doRequest} from "./APIFunktions"
import {UserContext,Login} from "../UserContext";

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
