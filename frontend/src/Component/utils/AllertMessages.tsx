import React, {useContext} from "react";
import {Message, MessageContext} from "../../context/MessageContext";
import SnackbarContent from "@mui/material/SnackbarContent";
import Stack from "@mui/material/Stack";


export default function AlertMassages() {

  const messageContext = useContext(MessageContext)

  return (
      <Stack spacing={2} sx={{ maxWidth: 600 }}>
        {messageContext.messagesArrayWrapper.arr.map((m:Message,i:number)=>{
          return <SnackbarContent key={i} message={m.text}/>
        })}
      </Stack>
  );
}
