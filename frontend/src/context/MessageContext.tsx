import {createContext} from "react";
import {AlertColor} from "@mui/material";

export interface Message{
  text: string;
  type?: AlertColor;
  closeTime?: number;
}

export interface MessagesArrayWrapper{
  arr:Message[];
}

function createMessagesArrayWrapper():MessagesArrayWrapper{
  return { arr: []};
}

const messagesArrayWrapperProps = {
  messagesArrayWrapper: createMessagesArrayWrapper(),
  setMessagesArrayWrapper: (message: MessagesArrayWrapper) => {} // noop default callback
};

export const MessageContext = createContext(messagesArrayWrapperProps);


