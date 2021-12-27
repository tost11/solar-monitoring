import React, {useContext} from "react";
import {UserContext} from "../UserContext";

// @ts-ignore
import AuthIFrame from "react-auth-iframe";

export default function TestComponent() {

  const login = useContext(UserContext);

  return <div>
    <AuthIFrame
        src="http://localhost:1234/api/graphs/dashboard-solo/new?orgId=1&panelId=2" width="450" height="200"
        token={login?.jwt}/>
  </div>
}