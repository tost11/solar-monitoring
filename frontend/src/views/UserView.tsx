import React, {useContext, useEffect, useState} from "react";
import {UserContext} from "../context/UserContext";
import {
  apiCreateNotification,
  apiDeleteNotification,
  getUser,
  UserDTO
} from "../api/UserAPIFunctions";
import Button from "@mui/material/Button";
import {MenuItem, OutlinedInput, TextField} from "@mui/material";
import Select from "@mui/material/Select";

export default function UserView() {

  const login = useContext(UserContext);

  const [user,setUser] = useState<UserDTO>();
  const [selectedSystem,setSelectedSystem] = useState(0);
  const [selectedType,setSelectedType] = useState(0);
  const [value,setValue] = useState("");
  const [onCreation,setOnCreation] = useState(false);

  const types = ["Mail","UserMail"];

  useEffect(() => {
      getUser().then((res) => {
        setUser(res);
        if(res.accessSystems?.length>0){
          setSelectedSystem(0)
        }
      })
  }, [])

  const NotificationList = (list)=>{
    return <div className={"flexColumnGap"}>
      {list.map((not,i)=><div key={i} style={{padding:"10px",borderRadius:"5px",margin:"auto",marginLeft:"10px",backgroundColor:"white"}}>
          {not.solarSystemName} ({not.solarSystemType}) {"<--"} {not.type} {not.type === "Mail"?"("+not.value+")":""}
          &nbsp;&nbsp;
          <Button variant="outlined"
            onClick={(ev)=>deleteNotification(not.id)}
            disabled={onCreation}>Remove</Button>
        </div>)}
    </div>
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
      <div>
        Solarsystem: <Select
          value={selectedSystem}
          onChange={(ev)=>{setSelectedSystem(+ev.target.value)}}
          input={<OutlinedInput label="Name" />}
        >
          {list.map((sys,i) => (
            <MenuItem
              key={i}
              value={i}
            >
              {sys.name} ({sys.name})
            </MenuItem>
          ))}
        </Select>
      </div>
      <div>
        Type: <Select
          value={selectedType}
          onChange={(ev)=>{setSelectedType(+ev.target.value)}}
          input={<OutlinedInput label="Name" />}
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
      </div>
      {types[selectedType] == "Mail" && <div>
        Username: <TextField className={"Input"}
           variant="outlined"
           value={value}
           onChange={(event) => {setValue(event.target.value)}}/>
      </div>}
      <div>
        <Button variant="contained"
            onClick={() => createNewNotification(list)}
            disabled={onCreation || (types[selectedType]== "Mail" && value == "")/*TODO regex*/}>Add</Button>
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


  return <div>

    {user ? <>

      <h2>User Information</h2>

      <h4>Basic Info</h4>

      <div className={"flex-content"}>
        <div>Id: {user?.id}</div>
        <div>Name: {user?.name}</div>
        <div>Num Systems: {user?.numAllowedSystems}</div>
      </div>

      <h4>Notifications</h4>
      {NotificationList(user.notifications)}

      {user.accessSystems?.length > 0 && <>
        <h4>Create new Notification</h4>
        <div className={"flexColumnGap"} style={{backgroundColor: "white",borderRadius:"5px",padding:"10px"}}>
          {PossibleNotificationSystems(user.accessSystems)}
        </div>

      </>}
    </>:
    <>
      Loading...
    </>}
  </div>
}
