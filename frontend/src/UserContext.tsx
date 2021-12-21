import {createContext} from "react";

export interface Login{
  name:string;
  id:number;
  jwt:string;
}
export interface IsLogin{
 login:boolean
}

export const UserContext = createContext<null|Login>(null);


export const IsLoginContext =createContext<null|IsLogin>(null);

