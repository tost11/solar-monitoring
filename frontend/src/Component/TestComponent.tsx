import React, {useContext} from "react";
import {UserContext} from "../UserContext";


// @ts-ignore
import AuthIFrame from "react-auth-iframe";


export default function TestComponent() {

  const login = useContext(UserContext);

  return <div>
    <iframe
        src="/grafana/d-solo/WbpO6gA7z/dashboard?orgId=1&from=1640695345289&to=1640697145289&panelId=10"
        width="450" height="200" frameBorder="0"/>
  </div>
}
