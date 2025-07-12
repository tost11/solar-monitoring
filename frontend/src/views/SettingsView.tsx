import React, {useEffect, useState} from "react";
import {editUserAdmin, findUsersForSettings, patchUser, UserDTO} from "../api/UserAPIFunctions";
import {Alert, Button, Stack, Switch, TextField, Typography} from "@mui/material";
import UserTable from "../Component/UserTable";
import {ConfigDTO, fetchApplicationConfig, fetchSetRegistration} from "../api/AdminApiFunctions";
import {useTranslation} from "react-i18next";

export default function SettingsView() {

  const { t } = useTranslation();

  const [selectUser, setSelectUser] = useState<UserDTO>()
  const [response, setResponse] = useState(false)
  const [userList, setUserList] = useState<UserDTO[]>([])
  const [searchName,setSearchName] = useState<string>("")
  const [timer,setTimer] = useState<NodeJS.Timeout|null>(null);
  const [config,setConfig] = useState<ConfigDTO>();

  const loadTable = () => {
    findUsersForSettings(searchName).then((r) => {
      {r != null &&
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
    <h1>{t("common.settings")}</h1>
    <h2>{t("views.settings.application_config")}</h2>
    {config && <div>
      {t("views.settings.registration_enabled")}:
      <Switch
          checked={config.isRegistrationEnabled}
          onChange={changeRegistration}
          inputProps={{ 'aria-label': 'controlled' }}
      />
    </div>}

    <h2>{t("views.settings.user_config")}</h2>

    {response && <Alert severity={"success"}>
      {selectUser}
    </Alert>}
    <TextField className={"Input"} type="text" name="UserName" value={searchName}
               placeholder="Search for User" onChange={(event) => {
      setSearchName(event.target.value as string)
      setSelectUser(undefined)
    }}/>
    {userList &&
      <UserTable userList={userList} setSelectUser={setSelectUser} selectUser={selectUser}/>
    }

    {selectUser && <div>
      <h2>{t("views.settings.edit_user")}</h2>
        <Stack direction="row" spacing={1} alignItems="center">
          <b>{t("common.username")}:</b>
          <TextField className={"Input"} type="text" value={selectUser.name} onChange={(event) => {
              // @ts-ignore
              setSelectUser(preventUser=>({
                ...preventUser,
                name:event.target.value as string
              }));
            }
          }/>
        </Stack>
      <Stack direction="row" spacing={1} alignItems="center">
        <b>{t("common.mail")}:</b>
        <TextField className={"Input"} type="text" value={selectUser.mail} onChange={(event) => {
          // @ts-ignore
          setSelectUser(preventUser=>({
            ...preventUser,
            name:event.target.value as string
          }));
        }
        }/>
      </Stack>
        <Stack direction="row" spacing={1} alignItems="center">
          <b>{t("views.settings.max_systems")}:</b>
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
        </Stack>

        <Stack direction="row" spacing={1} alignItems="center">
          <b>{t("views.settings.admin")}:</b>
          <Typography>{t("common.no")}</Typography>
          <Switch checked={selectUser.admin} onChange={() => {
            // @ts-ignore
            setSelectUser((preventUser) => ({
              ...preventUser,
              admin: !selectUser?.admin
            }))
          }}/>
          <Typography>{t("common.yes")}</Typography>
        </Stack>
        <Stack direction="row" spacing={1} alignItems="center">
          <b>{t("views.settings.deleted")}:</b>
          <Typography>{t("common.no")}</Typography>
          <Switch checked={selectUser.deleted} onChange={() => {
            // @ts-ignore
            setSelectUser((preventUser) => ({
              ...preventUser,
              deleted: !selectUser?.deleted
            }))
          }}/>
          <Typography>{t("common.yes")}</Typography>
        </Stack>

        <Button variant="outlined" onClick={() => {
          editUserAdmin(selectUser).then((r) => {
            loadTable()
            setSelectUser(undefined)
          })
        }}>{t("views.settings.save_user")}</Button>
      </div>
      }
    </div>
}
