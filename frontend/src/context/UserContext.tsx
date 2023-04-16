import {createContext} from "react";

export interface Login {
  name: string;
  id: string;
  jwt: string;
  admin: boolean;
}

export const UserContext = createContext<Login|undefined>(undefined);


