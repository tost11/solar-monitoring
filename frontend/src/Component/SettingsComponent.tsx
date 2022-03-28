import React, {useEffect, useState} from "react";
import {findUsersForSettings, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "./UserTable";
import {ConfigDTO, fetchApplicationConfig, fetchSetRegistration} from "../api/AdminApiFunctions";

export default function SettingsComponent() {
  const [selectUser, setSelectUser] = useState<UserDTO>()
  const [response, setResponse] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [userList, setUserList] = useState<UserDTO[]>([])
  const [searchName,setSearchName] = useState<string>("")
  const [timer,setTimer] = useState<NodeJS.Timeout|null>(null);
  const [config,setConfig] = useState<ConfigDTO>();

  const loadTable = () => {
    setIsAdmin(true)
    findUsersForSettings(searchName).then((r) => {
      {r != null &&
        setIsAdmin(true)
        setUserList(r)
      }
    })
  }
  const setNewTimer = (v:()=>void)=>{
    if(timer){
      clearTimeout(timer);
    }
    setTimer(setTimeout(v,400))
  }

  useEffect(()=>{
    fetchApplicationConfig().then(setConfig)
  },[])

  useEffect(() => {
    {searchName != "" &&
    setNewTimer(()=> {
      loadTable()
    })
    }
    {searchName == ""&&
    setUserList([])}

  }, [searchName])

  const changeRegistration = ()=>{
    // @ts-ignore
    let newVal = !config.isRegistrationEnabled;
    fetchSetRegistration(newVal).then(()=> {
      setConfig({...config, isRegistrationEnabled: newVal})
    })
  }

  return<div>
    <h1>Settings</h1>
    <h2>ApplicationConfig</h2>
    {config && <div>
      Registration enabled:
      <Switch
          checked={config.isRegistrationEnabled}
          onChange={changeRegistration}
          inputProps={{ 'aria-label': 'controlled' }}
      />
    </div>}

    <h2>Users config</h2>

    {response && <Alert severity={"success"}>
      {selectUser}
    </Alert>}
    <TextField className={"Input"} type="text" name="UserName" value={searchName}
               placeholder="Search for User" onChange={(event) => {
      setSearchName(event.target.value as string)
    }}/>
    {userList &&
      <UserTable userList={userList} setSelectUser={setSelectUser} selectUser={selectUser}/>
    }

    {selectUser && <div>
        <TextField className={"Input"} type="text" name="UserName" value={selectUser.name}
                   placeholder="Witch User make to Admin" onChange={(event) => {
          // @ts-ignore
          setSelectUser(preventUser=>({
            ...preventUser,
            name:event.target.value as string
          }));
        }
        }/>
        <TextField className={"Input"} type="number" name="numberOfMaxSystems" value={selectUser.numAllowedSystems}
                   placeholder="Witch User make to Admin" onChange={(event) => {
            {!isNaN(Number(event.target.value))&&
            // @ts-ignore
            setSelectUser(preventUser=> ({
              ...preventUser,
            numAllowedSystems: Number(event.target.value)
            }))
          }
        }}/>

        <h3>IsAdmin?</h3>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography>no</Typography>
          <Switch checked={selectUser.admin} onChange={() => {
            // @ts-ignore
            setSelectUser((preventUser) => ({
              ...preventUser,
              admin: !selectUser?.admin
            }))
          }}/>
          <Typography>yes</Typography>
        </Stack>

        <Button variant="outlined" onClick={() => {
          patchUser(selectUser).then((r) => {
            loadTable()

          })
        }}>Edit User</Button>
      </div>
      }
    </div>
}
