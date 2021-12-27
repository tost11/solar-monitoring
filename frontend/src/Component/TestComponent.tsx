import React, {useContext} from "react";
import {UserContext} from "../UserContext";


// @ts-ignore
import AuthIFrame from "react-auth-iframe";

export default function TestComponent() {

  const login = useContext(UserContext);

  return <div>
    <AuthIFrame
        src="http://localhost:1234/api/graphs/d-solo/WbpO6gA7z/dashboard?orgId=1&from=1640620427993&to=1640622227993&panelId=10" width="450" height="200"
        token={login?.jwt}/>
    <iframe src= "http://localhost:1234/d-solo/WbpO6gA7z/dashboard?orgId=1&from=1640620427993&to=1640622227993&panelId=10" width="450" height="200" ></iframe>
  </div>
}
