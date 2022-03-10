import {Checkbox, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import React, {useState} from "react";
import {UserDTO} from "../api/UserAPIFunctions";

interface TableBody{
  userList:UserDTO[]
  setSelectUser:(user:UserDTO|undefined)=>void
  selectUser?:UserDTO
}
export default function UserTable({userList,setSelectUser,selectUser}:TableBody){
  const [checked,setChecked]=useState(false)
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
              <TableCell>{row.numAllowedSystems.toString()}</TableCell>
              <Checkbox onChange={(event) => {
                {
                  !checked &&
                  setSelectUser(row);
                }
                {
                  checked &&
                  setSelectUser(undefined);
                }
                setChecked(!checked)
              }} disabled={checked && selectUser && selectUser.name != row.name}/>

            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    }
  </div>

}
