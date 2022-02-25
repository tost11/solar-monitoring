import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {findUser, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Input} from "@mui/icons-material";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "./UserTable";
import SearchUser from "./SearchUser";

export default function SettingsComponent() {
  const [selectUser, setSelectUser] = useState<UserDTO>({id:0,name:"",numAllowedSystems:0,admin:false})


  return<div>
    <SearchUser setUser={setSelectUser}/>
      {selectUser&&<div>

        <TextField className={"Input"} type="text" name="UserName" value={selectUser.name}
        placeholder="Witch User make to Admin" onChange={(event) => {
        setSelectUser(preventUser=>({
        ...preventUser,
        name:event.target.value as string
      }));
      }
      }/>
        <TextField className={"Input"} type="number" name="numberOfMaxSystems" value={selectUser.numAllowedSystems}
        placeholder="Witch User make to Admin" onChange={(event) => {
        console.log("vorher2 "+event.target.value)
      {!isNaN(Number(event.target.value))&&
        setSelectUser(preventUser=> ({
        ...preventUser,
        numAllowedSystems: Number(event.target.value)
      }));
      }
      }
      }/>

        <h3>IsAdmin?</h3>
        <Stack direction="row" spacing={1} alignItems="center">
        <Typography>no</Typography>
            <Switch checked={selectUser.admin} onChange={() => {
              setSelectUser(preventUser => ({
                ...preventUser,
                admin: !selectUser.admin
              }));
            }}/>
        <Typography>yes</Typography>
        </Stack>

        <Button variant="outlined" onClick={() => {
        patchUser(selectUser).then((r) => {

      })
      }}>Edit User</Button>
      </div>
      }


  </div>
}
