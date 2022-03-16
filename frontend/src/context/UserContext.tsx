import {createContext} from "react";

export interface Login {
  name: string;
  id: number;
  jwt: string;
  admin: boolean;
}

export const UserContext = createContext<null | Login>(null);


