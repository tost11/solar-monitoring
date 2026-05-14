import React, {useContext, useEffect, useState} from "react";
import {Login, UserContext} from "../context/UserContext";
import {
  apiCreateNotification,
  apiDeleteNotification, apiUpdateUser,
  getUser, UpdateUserDTO,
  UserDTO
} from "../api/UserAPIFunctions";
import {Button, FormControl, InputLabel, MenuItem, Stack, TextField, Select} from "@mui/material";
import {toast} from "react-toastify";
import {useTranslation} from "react-i18next";
import {isMailValid} from "../Component/utils/validation";
import LogoutComponent from "../Component/modal/LogoutComponent";
import DeleteUserModal from "../Component/modal/DeleteUserModal";
import {deleteSystem} from "../api/SolarSystemAPI";

interface LogoutProps {
  setLogin: (login?: Login) => void;
}


export default function UserView({setLogin}:LogoutProps) {

  const { t } = useTranslation()

  const login = useContext(UserContext);

  const [user,setUser] = useState<UserDTO>();
  const [selectedSystem,setSelectedSystem] = useState(0);
  const [selectedType,setSelectedType] = useState(0);
  const [value,setValue] = useState("");
  const [onCreation,setOnCreation] = useState(false);
  const [mail,setMail] = useState<string|undefined>();
  const [deleteUserModalOpen, setDeleteUserModalOpen] = useState(false)

  const types = ["Mail","UserMail"];


  useEffect(() => {
      getUser().then((res) => {
        setUser(res)
        setMail(res.mail)
        if(res.accessSystems?.length>0){
          setSelectedSystem(0)
        }
      })
  }, [])

  const NotificationList = (list)=>{
    return <>{list?.length > 0 ? <div className={"flexColumnGap"}>
      <h4>Create new Notification</h4>
      {list.map((not, i) => <div key={i} style={{
        padding: "10px",
        borderRadius: "5px",
        margin: "auto",
        marginLeft: "10px",
        backgroundColor: "white"
      }}>
        {not.solarSystemName} ({not.solarSystemType}) {"<--"} {not.type} {not.type === "Mail" ? "(" + not.value + ")" : ""}
        &nbsp;&nbsp;
        <Button variant="outlined"
                onClick={(ev) => deleteNotification(not.id)}
                disabled={onCreation}>Remove</Button>
      </div>)}
    </div> : <></>}</>
  }

  const createNewNotification = (list)=>{
    setOnCreation(true);
    apiCreateNotification({
      id:list[selectedSystem].id,
      type:types[selectedType],
      value:value
    }).then(res=>{
      var newUser = {...user} as UserDTO
      newUser.notifications.push(res);
      setUser(newUser);
      setOnCreation(false);
      setValue("");
    }).catch(()=>{
      setOnCreation(false);
    })
  }

  const deleteNotification = (id:string)=>{
    setOnCreation(true);
    apiDeleteNotification(id).then(res=>{
      var newUser = {...user} as UserDTO
      newUser.notifications = newUser.notifications.filter(v=>v.id != id)
      setUser(newUser);
      setOnCreation(false);
    }).catch(()=>{
      setOnCreation(false);
    })
  }

  const PossibleNotificationSystems = (list)=>{
    return <>
      <div className="defaultFlexRow">
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel id="system-select-label">{t("common.solar_system")}</InputLabel>
            <Select
              labelId="system-select-label"
              id="system-select"
              value={list && list.length > 0 ? selectedSystem : ""}
              label={t("common.solar_system")}
              onChange={(ev)=>{setSelectedSystem(+ev.target.value)}}
            >
              {list && list.map((sys,i) => (
                <MenuItem
                  key={i}
                  value={i}
                >
                  {sys.name} ({sys.type})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>
      </div>
      <div className="defaultFlexRow">
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel id="type-select-label">Type</InputLabel>
            <Select
              labelId="type-select-label"
              id="type-select"
              value={selectedType}
              label="Type"
              onChange={(ev)=>{setSelectedType(+ev.target.value)}}
            >
              {types.map((type,i) => (
                <MenuItem
                  key={i}
                  value={i}
                >
                  {type}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>
      </div>
      {types[selectedType] == "Mail" && <div className="defaultFlexRow">
        <TextField className={"Input"}
           label={t("common.mail")}
           variant="outlined"
           value={value}
           onChange={(event) => {setValue(event.target.value)}}/>
      </div>}
      <div>
        <Button variant="contained"
            onClick={() => createNewNotification(list)}
            disabled={onCreation || (types[selectedType] == "Mail" && !isMailValid(mail))}>{t("common.add")}</Button>
      </div>
    </>


    /*return <div className="flexColumnGap">
      {sys.map(s=>
        <div style={{marginRight:"auto"}}>
          <div style={{padding:"5px",borderRadius: "5px",backgroundColor:"white"}}>
            {s.name} ({s.type}) <Button>Add</Button>
          </div>
        </div>
      )}
    </div>*/
  }

  /* reimplement new
  const updateUser = ()=>{
    apiUpdateUser({
      mail: mail===""?null:mail
    } as UpdateUserDTO).then(res=>{
      setOnSaveUser(false);
      toast.success("User successfully updated")
    }).catch(()=>{
      setOnSaveUser(false);
    })
  }*/

  return <div>

    {user ? <>

      <h2>{t("views.profile.info")}</h2>

      <h4>{t("views.profile.profile")}</h4>

      <div className={"flexColumnGap"}>
        <div>Id: {user.id}</div>
        <div>{t("common.name")}: {user.name}</div>
        <div>{t("views.profile.num_systems")}: {user.numAllowedSystems}</div>
        <div>{t("common.mail")}: {user.mail}</div>
        <div>
          <Button style={{margin:"auto"}} onClick={() => setDeleteUserModalOpen(true)} variant="contained">
          {t("views.profile.delete_user")}
        </Button>
        </div>
        {/*<TextField style={{marginRight:"auto"}} className={"Input"} label="Mail" variant="outlined" value={mail?mail:""} onChange={(event) => {
          setMail(event.target.value)
        }}/>
        <Button style={{marginRight:"auto"}} variant="contained"
                onClick={updateUser}
                disabled={onSaveUser}
        >Save User</Button>*/}
      </div>

      {NotificationList(user.notifications)}

      {user.accessSystems?.length > 0 && <>
        <h4>{t("views.profile.new_notification")}</h4>
        <div className={"flexColumnGap"} style={{backgroundColor: "white",borderRadius:"5px",padding:"10px"}}>
          {PossibleNotificationSystems(user.accessSystems)}
        </div>

      </>}
    </>:
    <>
      {t("views.profile.loading")}
    </>}
    <DeleteUserModal open={deleteUserModalOpen} onClose={() => setDeleteUserModalOpen(false)} setLogin={setLogin}/>
  </div>
}
