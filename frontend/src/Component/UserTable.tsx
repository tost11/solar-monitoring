import {Button, Checkbox, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import React, {useState} from "react";
import {UserDTO} from "../api/UserAPIFunctions";
import {useTranslation} from "react-i18next";

interface TableBody{
  userList:UserDTO[]
  setSelectUser:(user:UserDTO|undefined)=>void
  selectUser?:UserDTO
}
export default function UserTable({userList,setSelectUser}:TableBody){

  const { t } = useTranslation();

  return<div style={{overflow:"scroll",maxHeight:"400px",width:"40%"}}>
    {userList.length > 0 ?
    <>
      <h4>{t("components.user_table.found_users")}</h4>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell> {t("common.name")} </TableCell>
              <TableCell> {t("components.user_table.max_systems")} </TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
        <TableBody>
          {userList.map((row) => (
            <TableRow key={row.name}
            >
              <TableCell>{row.name}</TableCell>
              <TableCell>{row.numAllowedSystems.toString()}</TableCell>
              <TableCell>
                <Button variant="contained" onClick={()=> setSelectUser(row)}>
                  {t("common.edit")}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    </>:
    <div>
      <h4>{t("components.user_table.no_users")}</h4>
    </div>
    }
  </div>

}
