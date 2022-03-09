import React, {useEffect, useState} from "react";
import {findUsersForSettings, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "./UserTable";

export default function SettingsComponent() {
  const [selectUser, setSelectUser] = useState<UserDTO>()
  const [response, setResponse] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [userList, setUserList] = useState<UserDTO[]>()
  const [searchName,setSearchName] = useState<string>("")

  const loadTable = () => {
    setIsAdmin(true)
    findUsersForSettings(searchName).then((r) => {
      {r != null &&
        setIsAdmin(true)
        setUserList(r)
        console.log(r)
      }
    })
  }

  useEffect(() => {
    {searchName != "" &&
      loadTable()
    }
    {searchName == ""&&
    setUserList([])}

  }, [searchName])

  return<div>
    {response && <Alert severity={"success"}>
      {selectUser}
    </Alert>}
    <TextField className={"Input"} type="text" name="UserName" value={searchName}
               placeholder="Search for USer" onChange={(event) => {
      setSearchName(event.target.value as string)
    }}/>


    {userList &&
      <UserTable userList={userList} setSelectUser={setSelectUser} selectUser={selectUser}/>
    }

    {selectUser && <div>
        <TextField className={"Input"} type="text" name="UserName" value={selectUser.name}
                   placeholder="Witch User make to Admin" onChange={(event) => {
          setSelectUser(preventUser=>({
            ...preventUser,
            name:event.target.value as string
          }));
          console.log(selectUser.name)
        }
        }/>
        <TextField className={"Input"} type="number" name="numberOfMaxSystems" value={selectUser.numAllowedSystems}
                   placeholder="Witch User make to Admin" onChange={(event) => {
            console.log("vorher "+event.target.value)
            {!isNaN(Number(event.target.value))&&
            setSelectUser(preventUser=> ({
              ...preventUser,
            numAllowedSystems: Number(event.target.value)
            }))
          }
        }}/>

        <h3>IsAdmin?</h3>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography>no</Typography>
          {selectUser.admin?<Switch defaultChecked onChange={() => {
            setSelectUser(preventUser=> ({
              ...preventUser,
              admin: false
            }))
          }}/>:<Switch onChange={() => {
            setSelectUser(preventUser=> ({
              ...preventUser,
              admin: true
            }))
          }}/>
          }
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
