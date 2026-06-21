import {doRequest, doRequestNoBody} from "./APIFunktions"
import {Login} from "../context/UserContext";
import moment from "moment";

export interface LoginDTO{
  name:string;
  password:string;
}

export interface NotificationDTO{
  id: string,
  value: string,
  type: string,
  solarSystemName: string,
  SolarSystemType: string
}

export interface UpdateNotificationDTO{
  id: string,
  value: string,
  type: string,
}

export interface UserAccessSystemDTO{
  id: string,
  name: string,
  type: string
}

export interface UserDTO{
  id:number,
  name:string,
  numAllowedSystems: number,
  //backend is isDeleted and isAdmin lombock changes ist autmatocly -.-
  admin:boolean,
  deleted:boolean,
  notifications: NotificationDTO[]
  accessSystems: UserAccessSystemDTO[]
  mail?: string
}

export interface UserAdminDTO{
  id:number,
  name:string,
  numAllowedSystems: number,
  admin:boolean,
  deleted:boolean,
  mail?: string,
  creationDate: moment;
}

export interface UpdateUserDTO{
  mail?:string
}

export interface GenericDataDTO{
  id:string,
  name:string
}

export interface TagDTO{
  id:string,
  name:string,
  color:string,
}


export interface AdminTagDTO{
  id:string,
  name:string,
  color:string,
  locked: boolean,
  viewOnStart: boolean,
  showStartPageAggregation: boolean
}

export interface RegisterInfo{
  captcha:string
}

export function getRegistrationInfo():Promise<RegisterInfo>{
  return doRequest<RegisterInfo>(window.location.origin+"/api/user/register","GET")
}

export function postLogin(name:string,password:string):Promise<Login>{
  let body={name,password};
  return doRequest<Login>(window.location.origin+"/api/user/login","Post",body)
}

export function postRegister(mail:string|undefined,name:string|undefined,password:string|undefined,captcha:string|undefined,captchaText:string|undefined): Promise<void>{
  let body = {mail,name, password,captcha,captchaText};
  return doRequestNoBody(window.location.origin + "/api/user/register", "POST", body)
}

export function findUsers(name:string):Promise<GenericDataDTO[]>{
 return doRequest<GenericDataDTO[]>(window.location.origin+"/api/user/findUser/"+name,"GET")
}

export function findUsersForSettings(name:string):Promise<UserDTO[]>{
  return doRequest<UserDTO[]>(window.location.origin+"/api/user/admin/findUser/"+name,"GET")
}

export function editUserAdmin(body:UserAdminDTO):Promise<UserAdminDTO>{
  return doRequest(window.location.origin+"/api/user/admin/edit", "POST",body)
}

export function getUser():Promise<UserDTO>{
  return doRequest<UserDTO>(window.location.origin+"/api/user", "GET")
}

export function apiUpdateUser(body:UpdateUserDTO):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/user", "POST",body)
}

export function apiCreateNotification(body:UpdateNotificationDTO):Promise<NotificationDTO>{
  return doRequest(window.location.origin+"/api/user/notification", "POST",body)
}

export function apiDeleteNotification(id:string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/user/notification?id="+id, "DELETE")
}

export function apiGetAvailableTags():Promise<TagDTO[]>{
  return doRequest(window.location.origin+"/api/tags", "GET")
}

export function apiGetTags():Promise<AdminTagDTO[]>{
  return doRequest(window.location.origin+"/api/tags", "GET")
}

export function apiCreateTag(body:TagDTO):Promise<AdminTagDTO>{
  return doRequest(window.location.origin+"/api/tags", "POST",body)
}

export function apiAddTagToSystem(systemId,tagId):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/tag?systemId="+systemId+"&tagId="+tagId, "POST")
}

export function apiRemoveTagFromSystem(systemId,tagId):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/tag?systemId="+systemId+"&tagId="+tagId, "DELETE")
}

export function apiLogout():Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/user/logout", "POST")
}

export function apiDeleteUser():Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/user", "DELETE")
}

export function apiDeleteSystem(id:String):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/"+id, "DELETE")
}

export function postPasswordResetRequest(dto: {usernameOrEmail: string, captchaImage: string, captchaText: string}): Promise<{message: string}> {
  return doRequest(window.location.origin + "/api/user/password-reset/request", "POST", dto);
}

export function validatePasswordResetToken(token: string): Promise<{valid: boolean}> {
  return doRequest(window.location.origin + "/api/user/password-reset/validate/" + token, "GET");
}

export function postPasswordResetConfirm(dto: {token: string, newPassword: string}): Promise<{message: string}> {
  return doRequest(window.location.origin + "/api/user/password-reset/confirm", "POST", dto);
}

