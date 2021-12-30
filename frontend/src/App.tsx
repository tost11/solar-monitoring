import React, {useEffect, useState} from "react";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import "./main.css"
import MenuBar from "./MenuBar"
import SystemComponent from "./Component/SystemComponent"
import CreateNewSystemComponent from "./Component/createANewSystemComponent";
import TestComponent from "./Component/TestComponent";
import {deleteCookie, getCookie, setCookie} from "./api/cookie";
import jwt_decode from "jwt-decode";
import StartPage from "./Component/StartPage"
import {Login, UserContext} from "./context/UserContext";
import {ToastContainer} from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';

interface Decoded {
  jti: string;
  sub: string;
}

export default function App() {

  const [login, setLogin] = useState<null | Login>(null);
  //const [messageArrayWrapper, setMessagesArrayWrapper] = useState<MessagesArrayWrapper>({arr:[]});

  //this is needet because when context changes this will be called again
  useEffect(()=>{
    console.log("reload page")
    let cookie = getCookie("jwt")
    if (cookie) {
      try {
        let decoded = jwt_decode<Decoded>(cookie)
        if (decoded.jti && !isNaN(Number(decoded.jti)) && decoded.sub) {
          setLogin({id: Number(decoded.jti), name: decoded.sub, jwt: cookie})
        }
      } catch (ex) {
        console.log("Could not parse last login cookie")
      }
    }
  },[])

  useEffect(() => {
    //this will be run when login context changes
    if (login) {
      setCookie("jwt", login.jwt, 30);
    } else {
      deleteCookie("jwt");
    }
  }, [login])

  return <div>
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
          <Routes>
            <Route path="/" element={<StartPage/>}/>
            <Route path="/systems" element={<SystemComponent/>}/>
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
      {/*</MessageContext.Provider>*/}
    </BrowserRouter>
    {/*router here*/}

    {/*regiester*/}
    {/*hauptseite*/}
    {/*configure*/}

  </div>
}
