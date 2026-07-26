import {doRequest, doRequestNoBody} from "./APIFunktions"
import moment from "moment";
import {TagDTO} from "./UserAPIFunctions";

export enum SolarSystemType {
  SELFMADE= "SELFMADE",
  SIMPLE = "SIMPLE",
  VERY_SIMPLE = "VERY_SIMPLE",
  GRID = "GRID",
  GRID_BATTERY = "GRID_BATTERY"
}

export interface BooleanStatus {
  name: string,
  lastSet: Date,
  value: boolean
}

export interface AllStatus {
  booleans: BooleanStatus[]
}

export enum SolarSystemPublicMode {
  NONE = "NONE",
  ALL = "ALL",
  PRODUCTION = "PRODUCTION"
}

export interface NamingsDTO {
  devices: {[key: string]: string}
  batteries: {[key: string]: string},
  inputsAC: {[key: string]: string},
  inputsDC: {[key: string]: string},
  outputsAC: {[key: string]: string}
  outputsDC: {[key: string]: string}
  grids: {[key: string]: string}
}

/*export interface NamingsMap{
  devices: Map<number,string>,
  batteries: Map<number,string>,
  inputs: Map<number,string>,
  outputs: Map<number,string>
}*/

export interface ViewData{
  hasTemperature?:boolean
  productionForTotalPricing?:boolean
  batteryVoltage?:number
  maxSolarVoltage?:number
  hideTotalConsumption?:boolean
  showGridInfo?:boolean
  totalPricingPublicOverride?:boolean
  defaultDelay?:number
  totalFilter?: string[]
  graphFilter?: string[]
}

export interface SystemInformationsDTO {
  name?: string;
  publicName?: string;
  description?: string;
  maxInstalledSolarPower?: number;
  maxInverterOutputPower?: number;
  batteryCapacity?: number;
  buildingDate?: string;
  electricityPrice?: number;
  electricityPriceFeedIn?: number;
}

export interface SolarSystemDTO{
  name: string,
  shortener: string,
  description?: string,
  buildingDate?:moment,
  creationDate:moment,
  type: SolarSystemType,
  id: string,
  electricityPrice?:number,
  electricityPriceFeedIn?:number,
  maxInstalledSolarPower?: number,
  maxInverterOutputPower?: number,
  batteryCapacity?: number,
  deyeSunSerialNumbers?:string,
  timezone: string,
  managers:ManagerDTO[],
  publicMode: SolarSystemPublicMode,
  status: AllStatus,
  publicFlagOnlyProduction: boolean,
  viewData: ViewData,
  namings: NamingsDTO,
  calculateCombinedValuesAfterwards?: boolean,
  tags: TagDTO[],
  systemInformations?: SystemInformationsDTO,
  viewName?: string,
}

export interface EditSolarSystemDTO {
  id?: string;
  token?: string;
  name: string;
  shortener?: string;
  type: SolarSystemType;
  timezone: string;
  publicMode: SolarSystemPublicMode;
  systemInformations: SystemInformationsDTO;
  viewData: ViewData;
  namings: NamingsDTO;
  deyeSunSerialNumbers?: string;
  calculateCombinedValuesAfterwards?: boolean;
  tokens?: AccessTokenResponseDTO[];
}

export interface CurrentValuesDTO{
  inputWatt?:number
  batteryVoltage?: number
}

export interface SolarSystemListDTO{
  name: string
  type: string
  id: string
  role:string
  shortener: string
  currentValues?:CurrentValuesDTO
  totalProducedWH? :number
  maxInstalledSolarPower?: number
  maxInverterOutputPower?: number
}

export interface TagSolarSystemDTO{
  tag: TagDTO
  systems: SolarSystemListDTO[]
}

export interface StartPageDataDTO{
  tagsWithSystems: TagSolarSystemDTO[]
  aggregationTags: TagDTO[]
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TagAggregationDTO {
  tag: TagDTO;
  totalSystems: number;
  onlineSystems: number;
  totalDayProducedKWH: number;
  totalDayConsumedKWH: number;
  totalCurrentProduction: number;
  totalCurrentConsumption: number;
  totalCurrentGrid: number;
  systems: PagedResponse<SystemContributionDTO>;
}

export interface SystemContributionDTO {
  id: string;
  name: string;
  type: string;
  isOnline: boolean;
  dayProducedKWH: number;
  dayConsumedKWH?: number;
  currentProduction: number;
  currentConsumption?: number;
  currentGrid?: number;
  role: string;
  maxInstalledSolarPower?: number;
}

export interface ManagerDTO{
  id:string,
  userName:string,
  role:string
}

export enum TokenPurpose {
  DATA_PUSH_REST = "DATA_PUSH_REST",
  DATA_PUSH_ENCRYPTED = "DATA_PUSH_ENCRYPTED"
}

export interface AccessTokenResponseDTO {
  id: string
  name: string
  purpose: TokenPurpose
  createdAt: string
  expiresAt?: string
}

export interface CreatedAccessTokenResponseDTO extends AccessTokenResponseDTO {
  token: string
}

export interface CreateAccessTokenDTO {
  name: string
  purpose: TokenPurpose
  expiresAt?: string
}

export interface UpdateAccessTokenDTO {
  name: string
  expiresAt?: string
  regenerateToken: boolean
}

export interface addMangerDTO{
  id:string
  systemId:string
  role:string
}

export interface MultSolarSystemDTO{
  name: string
  type: SolarSystemType
  id: string
  publicMode: SolarSystemPublicMode
  defaultDuration?: number
  viewName: string
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SolarSystemSearchParams{
  public?: boolean,
  name?: string,
  tags?: string[],
  type?: SolarSystemType,
  page?: number,
  size?: number,
  sortBy?: string,
  sortOrder?: string
}

export function getSystem(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/"+id,"GET")
}

export function getSystemInfo(id:string):Promise<SolarSystemDTO>{
  return doRequest<SolarSystemDTO>(window.location.origin+"/api/system/public/"+id,"GET")
}

export function searchSystems(search:SolarSystemSearchParams):Promise<PagedResponse<SolarSystemListDTO>>{
  return doRequest<PagedResponse<SolarSystemListDTO>>(window.location.origin+"/api/system/search","POST",search)
}

export function patchSystem(dto:EditSolarSystemDTO):Promise<ManagesSolarSystemDTO> {
  return doRequest(window.location.origin + "/api/system/edit", "POST", dto)
}

export function createSystem(dto:EditSolarSystemDTO):Promise<EditSolarSystemDTO> {
  return doRequest(window.location.origin + "/api/system", "POST", dto)
}

export function getSystemForEdit(systemId: string): Promise<EditSolarSystemDTO> {
  return doRequest(window.location.origin + "/api/system/edit/" + systemId, "GET")
}
export function deleteSystem(systemId:string){
  return doRequestNoBody(window.location.origin+"/api/system/delete/"+systemId,"POST")
}
export function getManagers(systemId:string):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/allManager/"+systemId,"GET")
}

export function setManageUser(manager:addMangerDTO):Promise<ManagerDTO[]>{
  return doRequest<ManagerDTO[]>(window.location.origin+"/api/system/addManageBy","POST",manager)
}
export function deleteMangerRelation(managerId:string,systemId:string):Promise<SolarSystemDTO>{
   return doRequest(window.location.origin+"/api/system/deleteManager/"+managerId+"/"+systemId,"POST")
}

export function createSystemToken(systemId: string, dto: CreateAccessTokenDTO): Promise<CreatedAccessTokenResponseDTO> {
  return doRequest<CreatedAccessTokenResponseDTO>(window.location.origin + "/api/system/tokens/" + systemId, "POST", dto)
}

export function deleteSystemToken(systemId: string, tokenId: string): Promise<void> {
  return doRequestNoBody(window.location.origin + "/api/system/tokens/" + systemId + "/" + tokenId, "DELETE")
}

export function updateSystemToken(systemId: string, tokenId: string, dto: UpdateAccessTokenDTO): Promise<AccessTokenResponseDTO | CreatedAccessTokenResponseDTO> {
  return doRequest<AccessTokenResponseDTO | CreatedAccessTokenResponseDTO>(window.location.origin + "/api/system/tokens/" + systemId + "/" + tokenId, "PATCH", dto)
}

export function updateStatistics(systemId:string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/statistics/"+systemId,"POST")
}

export function addBooleanStatus(systemId:string,name: string):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"PUT")
}

export function deleteBooleanStatus(systemId:string,name?: string):Promise<void>{
  return doRequestNoBody(window.location.origin+"/api/system/status/"+systemId+"?name="+name,"DELETE")
}

export function setBooleanStatus(systemId:string,name: string,value:boolean):Promise<BooleanStatus>{
  return doRequest<BooleanStatus>(window.location.origin+"/api/system/status/"+systemId+"?name="+name+"&value="+value,"POST")
}

export function apiGetTagAggregation(
  tagId: string,
  page: number = 0,
  size: number = 15,
  sortBy?: string,
  sortOrder: string = "asc"
): Promise<TagAggregationDTO> {
  let url = `${window.location.origin}/api/tags/aggregation/${tagId}?page=${page}&size=${size}`;
  if (sortBy) {
    url += `&sortBy=${sortBy}&sortOrder=${sortOrder}`;
  }
  return doRequest<TagAggregationDTO>(url, "GET");
}

export function getMultSystems(ids:string[],publicCall?:boolean):Promise<MultSolarSystemDTO[]>{
  return doRequest<MultSolarSystemDTO[]>(window.location.origin+"/api/system/"+(publicCall?"public/":"")+"mult?"+ids.map(s=>"systemIds="+s).join("&"),"GET")
}

export function getSystemsByTag():Promise<TagSolarSystemDTO[]>{
  return doRequest<TagSolarSystemDTO[]>(window.location.origin+"/api/tags/systems","GET")
}

export function getStartPageData():Promise<StartPageDataDTO>{
  return doRequest<StartPageDataDTO>(window.location.origin+"/api/tags/startpage","GET")
}

export function findTagsById(ids:String[]):Promise<TagDTO[]>{
  if(!ids || ids.length == 0) {
    return new Promise((resolve, _reject) => resolve([]));
  }
  const idString = ids.reduce(function (pre, next) {
    return pre + ',' + next;
  });
  return doRequest<TagDTO[]>(window.location.origin+"/api/tags/byIds?ids="+idString,"GET")
}
