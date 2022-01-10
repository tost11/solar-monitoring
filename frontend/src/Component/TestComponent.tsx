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
        src="grafana/d-solo/BBBBBBBBB/solar-selfmade-device-7031dd87-36c5-4494-a86a-721e82765a5c?orgId=1&refresh=10&theme=light&panelId=0" width="450" height="200" frameBorder="0"/>
  </div>
}
