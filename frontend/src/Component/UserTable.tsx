import {Checkbox, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import React, {useEffect, useState} from "react";
import {UserDTO} from "../api/UserAPIFunctions";
import {CheckBox} from "@material-ui/icons";

interface TableBody{
  userList:UserDTO[]
  setSelectUser:(user:UserDTO)=>void
  selectUser:UserDTO
}
export default function UserTable({userList,setSelectUser,selectUser}:TableBody){
  const [checked,setChecked]=useState(false)
  setSelectUser(selectUser)
  return<div style={{overflow:"scroll",maxHeight:"400px",width:"40%"}}>
    {userList.length > 0 &&
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell> Name
            </TableCell>
            <TableCell> Max Solar Systems
            </TableCell>
            <TableCell>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {userList.map((row) => (
            <TableRow key={row.name}
            >
              <TableCell>{row.name}</TableCell>
              <TableCell>{row.numbAllowedSystems.toString()}</TableCell>
              <Checkbox onChange={(event) => {
                {
                  !checked &&
                  setSelectUser(row);
                }
                {
                  checked &&
                  setSelectUser({name:"",numbAllowedSystems:0,admin:false});
                }
                setChecked(!checked)
              }} disabled={checked && selectUser.name != row.name}/>

            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    }
  </div>

}
