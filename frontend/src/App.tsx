import React, {useEffect, useState} from "react";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import "./main.css"
import {Login, UserContext} from "./UserContext"
import MenuBar from "./MenuBar"
import SystemComponent from "./Component/SystemComponent"
import CreateNewSystemComponent from "./Component/createANewSystemComponent";
import TestComponent from "./Component/TestComponent";
import {deleteCookie, getCookie, setCookie} from "./api/cookie";
import jwt_decode from "jwt-decode";

interface Decoded {
  jti: string;
  sub: string;
}

export default function App() {

  const loadLogin = () => {
    return getCookie('jwt')
  }


  let savedLogin = null;
  let cookie = getCookie("jwt");
  if (cookie) {
    try {
      let decoded = jwt_decode<Decoded>(cookie)
      if (decoded.jti && !isNaN(Number(decoded.jti)) && decoded.sub) {
        savedLogin = {id: Number(decoded.jti), name: decoded.sub, jwt: cookie}
      }
    } catch (ex) {
      console.log("Could not parse last login cookie")
    }
  }

  const [login, setLogin] = useState<null | Login>(savedLogin);


  useEffect(() => {
    //this will be run when login context changes
    if (login) {
      setCookie("jwt", login.jwt, 30);
    } else {
      deleteCookie("jwt");
    }
  }, [login])


  return <div>
    <BrowserRouter>
      <UserContext.Provider value={login}>
        <MenuBar setLogin={setLogin}/>
        <Routes>
          <Route path="/" element={<h1>Start</h1>}/>
          <Route path="/system" element={<SystemComponent setLogin={setLogin}/>}/>
          <Route path="/createNewSystem" element={<CreateNewSystemComponent/>}/>
          <Route path="/test" element={<TestComponent/>}/>
          <Route
              path="*"
              element={
                <main style={{padding: "1rem"}}>
                  <p>There's nothing here!</p>
                </main>
              }/>

        </Routes>
      </UserContext.Provider>
    </BrowserRouter>
    {/*router here*/}

    {/*regiester*/}
    {/*hauptseite*/}npm
    {/*configure*/}


  </div>
}
