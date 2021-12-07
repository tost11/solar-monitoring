import { createContext } from "react";
import {Login} from "./LoginAPI"

export const UserContext = createContext<null|Login>(null);
