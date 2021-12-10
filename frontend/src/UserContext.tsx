import {createContext} from "react";

export interface Login{
  name:string;
  id:number;
  jwt:string;
}
export interface SolarSystem{
  name:string;
  token:string;
}

export const UserContext = createContext<null|Login>(null);

export const SystemContext = createContext<null|SolarSystem>(null);

