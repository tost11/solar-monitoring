import React, {useEffect} from "react";
import {useState} from "react";
import {
  BrowserRouter,
  Routes,
  Route
} from "react-router-dom";
import "./main.css"
import {UserContext, Login} from "./UserContext"
import MenuBar from "./MenuBar"
import SystemComponent from "./Component/SystemComponent"
import CreateNewSystemComponent from "./Component/createANewSystemComponent";


export default function App() {

  const loadLogin = () => {
    try {
      const jsonValue = localStorage.getItem('login')
      return jsonValue ? JSON.parse(jsonValue) : null;
    } catch (e) {
      return null
    }
  }

  const [login, setLogin] = useState<null | Login>(loadLogin());

  useEffect(() => {

    //this will be run when login context changes
    if (login) {
      localStorage.setItem("login", JSON.stringify(login));
    } else {
      localStorage.removeItem("login")
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
    {/*hauptseite*/}
    {/*configure*/}


  </div>
}
