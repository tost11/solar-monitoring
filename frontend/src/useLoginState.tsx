import { useState } from "react"

export default function useLoginState() {
  const [isLogin,setLogin]=useState(false);

  return isLogin;
}
