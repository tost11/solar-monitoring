import React, {useContext} from "react";
import {UserContext} from "../context/UserContext";
import {Button} from "@mui/material";
import {toast} from "react-toastify";


export default function TestComponent() {

  const login = useContext(UserContext);

  return <div>
    <Button
        onClick={() => {
          toast('🦄 Wow so easy!',{hideProgressBar:false})
        }}
    >Test Alert</Button>
    <br/>
    <iframe
        src="/grafana/d-solo/WbpO6gA7z/dashboard?orgId=1&from=1640695345289&to=1640697145289&panelId=10"
        width="450" height="200" frameBorder="0"/>
  </div>
}
