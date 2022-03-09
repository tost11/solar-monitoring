import {TextField} from "@mui/material";
import Autocomplete from "@mui/material/Autocomplete";
import Stack from "@mui/material/Stack";
import React, {useEffect, useState} from "react";
import {findUser, UserDTO} from "../api/UserAPIFunctions";

interface SearchUserProps {
  setUser?: (v: UserDTO) => void
}

export default function SearchUser({setUser}: SearchUserProps) {
  const [params, setParams] = useState("");
  const [userList, setUserList] = useState<UserDTO[]>([])
  const [selected, setSelected] = useState<UserDTO | null>(null);
  useEffect(() => {
    {params != "" &&
      findUser(params).then((r) => {
        setUserList(r);
      })
      let user = userList.find(value => (value.name) === params)

      {
        user &&
        setSelected(user)
      }
    }

  }, [params])

  useEffect(() => {
    console.log(selected)
    {
      setUser && selected &&
      setUser(selected)
    }


  }, [selected])
  return (
    <Stack spacing={2} sx={{width: 300}}>
      <Autocomplete
        value={selected}
        onChange={(event, newValue) => {
          console.log("input")
          if (typeof newValue === "string") {
            console.log("ok")
            newValue = newValue.trim();
            setParams(newValue)
            setTimeout(() => {
              let user = userList.find(value => (value.name) === newValue)
              {
                user != undefined &&
                setSelected({
                  id: user.id,
                  name: user.name,
                  numAllowedSystems: user.numAllowedSystems,
                  admin: user.admin,
                  deleted:user.deleted,
                })
                console.log(user)
              }
              {
                user == undefined && console.log("user wirh name not exist" + newValue)
                setSelected(null)
              }
            })
          }
          else {
            setSelected(newValue)
          }
        }}
        options={userList}
        getOptionLabel={(option) => {
          if (typeof option === 'string') {
            return option;
          }

          return option.name
        }}
        selectOnFocus
        clearOnBlur
        handleHomeEndKeys
        renderOption={(props, option) => <li {...props}>{option.name}</li>}
        sx={{width: 300}}
        freeSolo
        renderInput={(params) => <TextField {...params} label="Free solo dialog"
                                            onChange={(a) => setParams((a.target.value as string).trim())
                                            }/>}
      />
    </Stack>
  );
}
