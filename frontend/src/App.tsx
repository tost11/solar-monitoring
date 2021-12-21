import React from "react";
import { useState } from "react";
import "./main.css"
import LoginComponent from "./Component/LoginComponent"
import RegisterComponent from "./Component/RegisterComponent"
import {UserContext,Login} from "./UserContext"
import MenuAppBar from "./MenuAppBar"
import SystemComponent from "./Component/SystemComponent"




export default function App() {
  const [login,setLogin]=useState<null|Login>(null);
  return <div>
    <UserContext.Provider value={login}>
      {/*router here*/}
      <MenuAppBar setLogin={setLogin}/>
      {/*regiester*/}

      {/*hauptseite*/}
      {/*configure*/}
        <RegisterComponent setLogin={setLogin}/>

    </UserContext.Provider>

  </div>
}
