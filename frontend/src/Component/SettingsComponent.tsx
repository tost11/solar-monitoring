import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {getAllUser, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Input} from "@mui/icons-material";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "./UserTable";

export default function SettingsComponent() {
  let initialState = {
    name: "",
    numbAllowedSystems: 0,
    admin: false,
  }
  const [selectUser, setSelectUser] = useState<UserDTO>({name:"",numbAllowedSystems:0,admin:false})
  const [response, setResponse] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [userList, setUserList] = useState<UserDTO[]>([])

  const loadTable = () => {
    getAllUser().then((r) => {
      {r != null &&
        setIsAdmin(true)
        setUserList(r)
        console.log(r)
      }
    })
  }

  useEffect(() => {
    loadTable()

  }, [])

  return<div>
    {response && <Alert severity={"success"}>
      {selectUser}
    </Alert>}
    {isAdmin ? <div>
      {userList &&
        <UserTable userList={userList} setSelectUser={setSelectUser} selectUser={selectUser}/>
      }

      {selectUser.name!=""&& <div>

        <TextField className={"Input"} type="text" name="UserName" value={selectUser.name}
                   placeholder="Witch User make to Admin" onChange={(event) => {
          setSelectUser(preventUser=>({
            ...preventUser,
            name:event.target.value as string
          }));
          console.log(selectUser.name)
        }
        }/>
        <TextField className={"Input"} type="number" name="numberOfMaxSystems" value={selectUser.numbAllowedSystems}
                   placeholder="Witch User make to Admin" onChange={(event) => {
                     console.log("vorher "+event.target.value)
          {!isNaN(Number(event.target.value))&&
            setSelectUser(preventUser=> ({
              ...preventUser,
            numbAllowedSystems: Number(event.target.value)
            }));
          }
          }
        }/>

        <h3>IsAdmin?</h3>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography>no</Typography>
          {selectUser.admin?<Switch defaultChecked onChange={() => {
            setSelectUser(preventUser=> ({
              ...preventUser,
              admin: false
            }));
          }}/>:<Switch onChange={() => {
            setSelectUser(preventUser=> ({
              ...preventUser,
              admin: true
            }));
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
    </div> : <div>
      no Admin
    </div>}

  </div>
}
