import React from "react";
import {Login} from "../UserContext";
import {doRequest} from "./APIFunktions"


export interface RegisterDTO {
  name: string;
  password: string;
}

export function postRegister(): (body: RegisterDTO) => Promise<Login> {
  return doRequest<Login>(window.location.origin + "/api/user/register", "POST")

}
