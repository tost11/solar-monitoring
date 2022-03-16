import React, {useEffect, useState} from "react"
import {BrowserRouter, Route, Routes} from "react-router-dom"
import "./main.css"
import MenuBar from "./MenuBar"
import SystemComponent from "./Component/SystemComponent"
import {deleteCookie, getCookie, setCookie} from "./api/cookie"
import jwt_decode from "jwt-decode";
import StartPage from "./Component/StartPage"
import {Login, UserContext} from "./context/UserContext";
import {ToastContainer} from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';
import {CircularProgress} from "@mui/material";
import DetailDashboard from "./Component/DetailDashboard";
import CreateNewSystemComponent from "./Component/CreateNewSystemComponent";
import EditSystemComponent from "./Component/EditSystemComponent";
import SettingsComponent from "./Component/SettingsComponent";
import Test from "./Component/Test"
import Graph from "./Component/Test";

interface Decoded {
  jti: string;
  sub: string;
  admin: boolean;
}

export default function App() {

  const [login, setLogin] = useState<null | Login>(null);
  //const [messageArrayWrapper, setMessagesArrayWrapper] = useState<MessagesArrayWrapper>({arr:[]});
  const [sessionLoaded,setSessionLoaded] = useState(false)
  //this is needed because when context changes this will be called again
  useEffect(()=>{
    let cookie = getCookie("jwt")
    if (cookie) {
      try {
        let decoded = jwt_decode<Decoded>(cookie)
        if (decoded.jti && !isNaN(Number(decoded.jti)) && decoded.sub) {
          setLogin({id: Number(decoded.jti), name: decoded.sub, jwt: cookie,admin: decoded.admin})
        }
      } catch (ex) {
        console.log("Could not parse last login cookie")
      }
    }
    setSessionLoaded(true)
  },[])

  useEffect(() => {
    //this will be run when login context changes
    if (login) {
      setCookie("jwt", login.jwt, 30);
    } else {
      deleteCookie("jwt");
    }
    setSessionLoaded(true)
  }, [login])

  return <div>
    {sessionLoaded ? <div>
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
            <MenuBar setLogin={setLogin}/>
            {login ? <Routes>
              <Route path="/" element={<StartPage/>}/>
              <Route path="/system" element={<SystemComponent/>}/>
              <Route path="/createNewSystem" element={<CreateNewSystemComponent/>}/>
              <Route path="/detailDashboard/:id" element={<DetailDashboard/>}/>
              <Route path="/edit/System/:id" element={<EditSystemComponent/>}/>
              <Route path="/Settings" element={<SettingsComponent/>}/>
              <Route
                path="*"
                element={
                  <main style={{padding: "1rem"}}>
                    <h1>404</h1>
                    <p>There's nothing here!</p>
                  </main>
                }/>
            </Routes>:<Routes><Route path="*" element={<StartPage/>}/> </Routes>
            }
          </UserContext.Provider>
        {/*</MessageContext.Provider>*/}
      </BrowserRouter>
    </div>:  <CircularProgress />}

  </div>
}
