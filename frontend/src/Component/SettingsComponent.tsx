import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {findUser, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Input} from "@mui/icons-material";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "./UserTable";
import SearchUser from "./SearchUser";

export default function SettingsComponent() {
  let initialState = {
    name: "",
    numAllowedSystems: 0,
    admin: false,
  }
  const [selectUser, setSelectUser] = useState<UserDTO>({id:0,name:"",numAllowedSystems:0,admin:false})
  const [response, setResponse] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)
  const [userList, setUserList] = useState<UserDTO[]>([])
  const [searchName,setSearchName] = useState<string>("")

  const loadTable = () => {
    setIsAdmin(true)
    findUser(searchName).then((r) => {
      {r != null &&
        setIsAdmin(true)
        setUserList(r)
        console.log(r)
      }
    })
  }

  useEffect(() => {
    {searchName != ""&&
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
        <TextField className={"Input"} type="number" name="numberOfMaxSystems" value={selectUser.numAllowedSystems}
                   placeholder="Witch User make to Admin" onChange={(event) => {
                     console.log("vorher "+event.target.value)
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

  </div>
}
