import {createContext} from "react";
import { overwriteLogin } from "./overwriteLogin";

export interface Login{
  name:string;
  id:number;
  jwt:string;
}


export const UserContext = createContext<null|Login>(null);


