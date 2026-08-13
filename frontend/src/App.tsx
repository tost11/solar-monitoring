import React, {lazy, Suspense, useState} from "react"
import {BrowserRouter, Route, Routes} from "react-router-dom"
import "./main.css"
import MenuBar from "./MenuBar"
import {deleteCookie, getCookie, setCookie} from "./api/cookie"
import jwt_decode from "jwt-decode";
import {Login, UserContext} from "./context/UserContext";
import {ToastContainer} from "react-toastify";
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment';
import CircularProgress from "@mui/material/CircularProgress";

import 'react-toastify/dist/ReactToastify.css';

// Lazy load all route components
const SystemsView = lazy(() => import("./views/SystemsView"));
const StartPage = lazy(() => import("./views/StartPage"));
const DetailDashboard = lazy(() => import("./views/SystemDashboardView"));
const CreateSystemView = lazy(() => import("./views/CreateSystemView"));
const EditSystemView = lazy(() => import("./views/EditSystemView"));
const SettingsView = lazy(() => import("./views/SettingsView"));
const SystemCompareView = lazy(() => import("./views/SystemCompareView"));
const TestView = lazy(() => import("./views/TestView"));
const UserView = lazy(() => import("./views/UserView"));
const TagsView = lazy(() => import("./views/TagsView"));
const TagAggregationView = lazy(() => import("./views/TagAggregationView"));
const ImpressumView = lazy(() => import("./views/ImpressumView"));
const PrivacyPolicyView = lazy(() => import("./views/PrivacyPolicyView"));
const PasswordResetPage = lazy(() => import("./views/PasswordResetPage"));

interface Decoded {
  jti: string;
  sub: string;
  admin: boolean;
}

export default function App() {

  let initLogin:Login|undefined = undefined;
  let cookie = getCookie("jwt")
  //console.log("coockie is: ",cookie)
  if (cookie) {
    try {
      let decoded = jwt_decode<Decoded>(cookie)
      if (decoded.jti && decoded.sub) {
        initLogin = {id: decoded.jti, name: decoded.sub, jwt: cookie,admin: decoded.admin};
      }
    } catch (_ex) {
      console.log("Could not parse last login cookie")
    }
  }

  const [login, setLogin] = useState<Login|undefined>(initLogin);

  const internSetLogin = (l?:Login) => {
    //console.log("set login ",l)
    if (l && l?.jwt) {
      setCookie("jwt", l.jwt, 30);
    } else {
      deleteCookie("jwt");
    }
    setLogin(l)
  }

  return <div>
    <LocalizationProvider dateAdapter={AdapterMoment}>
      <div>
        <ToastContainer
            position="top-center"
            autoClose={5000}
            hideProgressBar
            newestOnTop={false}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
        />
        <BrowserRouter>
            <UserContext.Provider value={login}>
              <MenuBar setLogin={internSetLogin}/>
              <Suspense fallback={
                <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: '80vh'}}>
                  <CircularProgress />
                </div>
              }>
                {login ? <Routes>
                  <Route path="/systems" element={<SystemsView/>}/>
                  <Route path="/createNewSystem" element={<CreateSystemView/>}/>
                  <Route path="/detailDashboard/:id" element={<DetailDashboard/>}/>
                  <Route path="/dd/:id" element={<DetailDashboard/>}/>
                  <Route path="/edit/System/:id" element={<EditSystemView/>}/>
                  <Route path="/Settings" element={<SettingsView/>}/>
                  <Route path="/compare" element={<SystemCompareView/>}/>
                  <Route path="/user" element={<UserView setLogin={setLogin}/>}/>
                  <Route path="/test" element={<TestView/>}/>
                  <Route path="/tags" element={<TagsView/>}/>
                  <Route path="/tag/:tagId" element={<TagAggregationView/>}/>
                  <Route path="/impressum" element={<ImpressumView/>}/>
                  <Route path="/privacypolicy" element={<PrivacyPolicyView/>}/>
                  <Route path="/" element={<StartPage/>}/>
                  <Route
                    path="*"
                    element={
                      <main style={{padding: "1rem"}}>
                        <h1>404</h1>
                        <p>There&apos;s nothing here!</p>
                      </main>
                    }/>
                </Routes>:<Routes>
                  <Route path="/detailDashboard/:id" element={<DetailDashboard/>}/>
                  <Route path="/dd/:id" element={<DetailDashboard/>}/>
                  <Route path="/compare" element={<SystemCompareView/>}/>
                  <Route path="/systems" element={<SystemsView/>}/>
                  <Route path="/tag/:tagId" element={<TagAggregationView/>}/>
                  <Route path="/impressum" element={<ImpressumView/>}/>
                  <Route path="/privacypolicy" element={<PrivacyPolicyView/>}/>
                  <Route path="/reset-password" element={<PasswordResetPage/>}/>
                  <Route path="*" element={<StartPage/>}/> </Routes>
                }
              </Suspense>
            </UserContext.Provider>
        </BrowserRouter>
      </div>
    </LocalizationProvider>
  </div>
}
