import React, {useEffect, useState} from "react"
import {BrowserRouter, Route, Routes} from "react-router-dom"
import "./main.css"
import MenuBar from "./MenuBar"
import SystemsView from "./views/SystemsView"
import {deleteCookie, getCookie, setCookie} from "./api/cookie"
import jwt_decode from "jwt-decode";
import StartPage from "./views/StartPage"
import {Login, UserContext} from "./context/UserContext";
import {ToastContainer} from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';
import {CircularProgress} from "@mui/material";
import DetailDashboard from "./views/SystemDashboardView";
import CreateSystemView from "./views/CreateSystemView";
import EditSystemView from "./views/EditSystemView";
import SettingsView from "./views/SettingsView";
import {LocalizationProvider} from "@mui/lab";
import DateAdapter from "@mui/lab/AdapterMoment";
import {LoginDTO} from "./api/UserAPIFunctions";

interface Decoded {
  jti: string;
  sub: string;
  admin: boolean;
}

export default function App() {

  let initLogin:Login|undefined = undefined;
  let cookie = getCookie("jwt")
  console.log("coockie is: ",cookie)
  if (cookie) {
    try {
      let decoded = jwt_decode<Decoded>(cookie)
      if (decoded.jti && !isNaN(Number(decoded.jti)) && decoded.sub) {
        initLogin = {id: Number(decoded.jti), name: decoded.sub, jwt: cookie,admin: decoded.admin};
      }
    } catch (ex) {
      console.log("Could not parse last login cookie")
    }
  }

  const [login, setLogin] = useState<Login|undefined>(initLogin);

  const internSetLogin = (l?:Login) => {
    console.log("set login ",l)
    if (l && l?.jwt) {
      setCookie("jwt", l.jwt, 30);
    } else {
      deleteCookie("jwt");
    }
    setLogin(l)
  }

  return <div>
    <LocalizationProvider dateAdapter={DateAdapter}>
      <div>
        <ToastContainer
            position="top-center"
            autoClose={5000}
            hideProgressBar={true}
            newestOnTop={false}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
        />
        <BrowserRouter>
          {/* <MessageContext.Provider value={{messagesArrayWrapper: messageArrayWrapper, setMessagesArrayWrapper:setMessagesArrayWrapper}}>
            <AlertMassages/>*/}
            <UserContext.Provider value={login}>
              <MenuBar setLogin={internSetLogin}/>
              {login ? <Routes>
                <Route path="/systems" element={<SystemsView/>}/>
                <Route path="/createNewSystem" element={<CreateSystemView/>}/>
                <Route path="/detailDashboard/:id" element={<DetailDashboard/>}/>
                <Route path="/edit/System/:id" element={<EditSystemView/>}/>
                <Route path="/Settings" element={<SettingsView/>}/>
                <Route path="/" element={<StartPage/>}/>
                <Route
                  path="*"
                  element={
                    <main style={{padding: "1rem"}}>
                      <h1>404</h1>
                      <p>There's nothing here!</p>
                    </main>
                  }/>
              </Routes>:<Routes>
                <Route path="/detailDashboard/:id" element={<DetailDashboard/>}/>
                <Route path="*" element={<StartPage/>}/> </Routes>
              }
            </UserContext.Provider>
          {/*</MessageContext.Provider>*/}
        </BrowserRouter>
      </div>
    </LocalizationProvider>
  </div>
}
