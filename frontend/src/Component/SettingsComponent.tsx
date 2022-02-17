import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {isUserAdmin, makeUserToAdmin} from "../api/UserAPIFunctions";
import { Input } from "@mui/icons-material";
import {Alert, Button, TextField} from "@mui/material";

export default function SettingsComponent(){
  const [userName,setUserName]=useState("")
  const [response,setResponse]=useState(false)

  const login=useContext(UserContext)

  return<div>
    {response&&<Alert  severity={"success"}>
      {userName}
    </Alert>}
    {login?.isAdmin?<div>

      <TextField className={"Input"} type="text" name="UserName" placeholder="Witch User make to Admin" onChange={(event) => {
    setUserName(event.target.value)
      }}/>
      <Button variant="outlined" onClick={()=>{
        makeUserToAdmin(userName).then((r)=>{
          setResponse(true)

        })
        }}>Make to Admin</Button>

    </div>:
      <div>
    you are not a admin
    </div>}

  </div>
}
