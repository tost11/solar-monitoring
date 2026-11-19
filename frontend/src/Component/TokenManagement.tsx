import React, {useContext, useState} from "react";
import {
  AccessTokenResponseDTO,
  CreateAccessTokenDTO,
  CreatedAccessTokenResponseDTO,
  UpdateAccessTokenDTO,
  TokenPurpose,
  ViewTokenLevel,
  ShareLinkCreateDTO,
  ShareLinkResponseDTO,
  createSystemToken,
  deleteSystemToken,
  updateSystemToken,
  createShareLink,
  listShareLinks,
  revokeShareLink,
  cleanupShareLink
} from "../api/SolarSystemAPI";
import {UserContext} from "../context/UserContext";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import LinkIcon from "@mui/icons-material/Link";
import BlockIcon from "@mui/icons-material/Block";
import {DatePicker, LocalizationProvider} from "@mui/x-date-pickers";
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {useTranslation} from "react-i18next";
import moment from "moment";
import {toast} from "react-toastify";

interface TokenManagementProps {
  systemId: string;
  initialTokens?: AccessTokenResponseDTO[];
}

function isExpired(token: AccessTokenResponseDTO): boolean {
  if (!token.expiresAt) return false;
  return new Date(token.expiresAt) < new Date();
}

function isLinkExpired(link: ShareLinkResponseDTO): boolean {
  if (!link.expiresAt) return false;
  return new Date(link.expiresAt) < new Date();
}

function formatDate(dateStr: string): string {
  return moment(dateStr).format("YYYY-MM-DD HH:mm");
}

const VIEW_LEVELS: ViewTokenLevel[] = ["ALL", "PRODUCTION"];

const VIEW_PASSWORD_PATTERN = /^(?=.*[A-ZÄÜÖ])(?=.*[a-zäüöß])(?=.*\d)(?=.*[@$%*#?!&])[A-ZÄÖÜa-zäöüß\d@$!%*#?&]{10,64}$/;

function levelLabel(level: ViewTokenLevel | undefined, t: (key: string) => string): string {
  if (level === "ALL") return t("views.edit_system.tokens_level_all");
  if (level === "PRODUCTION") return t("views.edit_system.tokens_level_production");
  return "—";
}

function purposeLabel(purpose: TokenPurpose, t: (key: string) => string): string {
  switch (purpose) {
    case TokenPurpose.DATA_PUSH_REST:
      return t("views.edit_system.tokens_purpose_rest");
    case TokenPurpose.DATA_PUSH_ENCRYPTED:
      return t("views.edit_system.tokens_purpose_encrypted");
    case TokenPurpose.VIEW:
      return t("views.edit_system.tokens_purpose_view");
    default:
      return purpose;
  }
}

export default function TokenManagement({systemId, initialTokens}: TokenManagementProps) {
  const {t} = useTranslation();
  const user = useContext(UserContext);
  const isAdmin = user?.admin === true;

  const NAME_PATTERN = /^[A-Za-z0-9_\-äüöÄÜÖßé ]{3,30}$/;

  const validateTokenName = (name: string): string | null => {
    const trimmed = name.trim();
    if (!trimmed) return t("views.edit_system.tokens_name_required");
    if (!NAME_PATTERN.test(trimmed)) return t("views.edit_system.tokens_name_invalid");
    return null;
  };

  const [tokens, setTokens] = useState<AccessTokenResponseDTO[]>(initialTokens || []);
  const [loading, setLoading] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [createdToken, setCreatedToken] = useState<CreatedAccessTokenResponseDTO | null>(null);

  // Create form state
  const [newName, setNewName] = useState("");
  const [newNameError, setNewNameError] = useState<string | null>(null);
  const [newPurpose, setNewPurpose] = useState<TokenPurpose>(TokenPurpose.DATA_PUSH_REST);
  const [newExpiry, setNewExpiry] = useState<moment.Moment | null>(null);

  // Edit form state
  const [editToken, setEditToken] = useState<AccessTokenResponseDTO | null>(null);
  const [editName, setEditName] = useState("");
  const [editNameError, setEditNameError] = useState<string | null>(null);
  const [editExpiry, setEditExpiry] = useState<moment.Moment | null>(null);
  const [regenerateConfirmStep, setRegenerateConfirmStep] = useState<0 | 1>(0);

  // Access links state
  const [shareLinks, setShareLinks] = useState<ShareLinkResponseDTO[]>([]);
  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [createdShareLink, setCreatedShareLink] = useState<ShareLinkResponseDTO | null>(null);
  const [cleanupLink, setCleanupLink] = useState<ShareLinkResponseDTO | null>(null);
  const [shareLevel, setShareLevel] = useState<ViewTokenLevel>("ALL");
  const [shareMaxUses, setShareMaxUses] = useState<string>("");
  const [shareExpiry, setShareExpiry] = useState<moment.Moment | null>(null);
  const [shareTokenExpiry, setShareTokenExpiry] = useState<moment.Moment | null>(null);
  const [shareNotes, setShareNotes] = useState("");
  const [shareSecretMode, setShareSecretMode] = useState<"generate" | "manual">("generate");
  const [shareSecret, setShareSecret] = useState("");
  const [shareSecretVisible, setShareSecretVisible] = useState(false);

  const loadShareLinks = () => {
    listShareLinks(systemId).then(setShareLinks).catch(() => {});
  };

  React.useEffect(() => {
    loadShareLinks();
  }, [systemId]);

  const handleCreate = () => {
    const error = validateTokenName(newName);
    if (error) {
      setNewNameError(error);
      return;
    }
    setLoading(true);
    const dto: CreateAccessTokenDTO = {
      name: newName.trim(),
      purpose: newPurpose,
      expiresAt: newExpiry ? newExpiry.toISOString() : undefined
    };
    createSystemToken(systemId, dto).then((res) => {
      setTokens([...tokens, res]);
      setCreateDialogOpen(false);
      setCreatedToken(res);
      setNewName("");
      setNewPurpose(TokenPurpose.DATA_PUSH_REST);
      setNewExpiry(null);
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleDelete = (tokenId: string) => {
    setLoading(true);
    deleteSystemToken(systemId, tokenId).then(() => {
      setTokens(tokens.filter(t => t.id !== tokenId));
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleOpenEdit = (token: AccessTokenResponseDTO) => {
    setEditToken(token);
    setEditName(token.name);
    setEditNameError(null);
    setEditExpiry(token.expiresAt ? moment(token.expiresAt) : null);
    setRegenerateConfirmStep(0);
  };

  const handleCloseEdit = () => {
    setEditToken(null);
    setRegenerateConfirmStep(0);
  };

  const handleSaveEdit = () => {
    if (!editToken) return;
    const error = validateTokenName(editName);
    if (error) {
      setEditNameError(error);
      return;
    }
    setLoading(true);
    const dto: UpdateAccessTokenDTO = {
      name: editName.trim(),
      expiresAt: editExpiry ? editExpiry.toISOString() : undefined,
      regenerateToken: false
    };
    updateSystemToken(systemId, editToken.id, dto).then((res) => {
      setTokens(tokens.map(t => t.id === editToken.id ? res : t));
      handleCloseEdit();
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleRegenerate = () => {
    if (!editToken) return;
    const error = validateTokenName(editName);
    if (error) {
      setEditNameError(error);
      return;
    }
    setLoading(true);
    const dto: UpdateAccessTokenDTO = {
      name: editName.trim(),
      expiresAt: editExpiry ? editExpiry.toISOString() : undefined,
      regenerateToken: true
    };
    updateSystemToken(systemId, editToken.id, dto).then((res) => {
      setTokens(tokens.map(t => t.id === editToken.id ? res : t));
      handleCloseEdit();
      // Show the new token in the created dialog
      if ('token' in res) {
        setCreatedToken(res as CreatedAccessTokenResponseDTO);
      }
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleCopyToken = () => {
    if (createdToken?.token) {
      navigator.clipboard.writeText(createdToken.token).then(() => {
        toast.success(t("views.edit_system.tokens_copied_clipboard"));
      });
    }
  };

  const buildShareLinkUrl = (secret: string) =>
    window.location.origin + "/share/" + systemId + "/" + encodeURIComponent(secret);

  const handleCreateShareLink = () => {
    const maxUses = shareMaxUses.trim() === "" ? undefined : parseInt(shareMaxUses, 10);
    if (maxUses !== undefined && (isNaN(maxUses) || maxUses <= 0)) return;
    if (shareSecretMode === "manual" && !VIEW_PASSWORD_PATTERN.test(shareSecret)) return;
    const dto: ShareLinkCreateDTO = {
      level: shareLevel,
      maxUses,
      secret: shareSecretMode === "manual" ? shareSecret : undefined,
      expiresAt: shareExpiry ? shareExpiry.toISOString() : undefined,
      tokenExpiresAt: shareTokenExpiry ? shareTokenExpiry.toISOString() : undefined,
      notes: shareNotes.trim() === "" ? undefined : shareNotes.trim()
    };
    setLoading(true);
    createShareLink(systemId, dto).then((res) => {
      setShareDialogOpen(false);
      setCreatedShareLink(res);
      setShareLevel("ALL");
      setShareMaxUses("");
      setShareExpiry(null);
      setShareTokenExpiry(null);
      setShareNotes("");
      setShareSecretMode("generate");
      setShareSecret("");
      setShareSecretVisible(false);
      loadShareLinks();
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleRevokeShareLink = (linkId: string) => {
    setLoading(true);
    revokeShareLink(systemId, linkId).then(() => {
      loadShareLinks();
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleCleanupShareLink = () => {
    if (!cleanupLink) return;
    setLoading(true);
    cleanupShareLink(systemId, cleanupLink.id).then(() => {
      setShareLinks(shareLinks.filter(l => l.id !== cleanupLink.id));
      setCleanupLink(null);
      setLoading(false);
    }).catch(() => {
      setLoading(false);
    });
  };

  const handleCopyShareLink = (url: string) => {
    navigator.clipboard.writeText(url).then(() => {
      toast.success(t("views.edit_system.sharelinks_copied_clipboard"));
    });
  };

  return (
    <div>
      <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px"}}>
        <h3 style={{margin: 0}}>{t("views.edit_system.tokens_header")}</h3>
        <Button variant="contained" onClick={() => setCreateDialogOpen(true)}>
          {t("views.edit_system.tokens_create")}
        </Button>
      </div>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t("views.edit_system.tokens_table_name")}</TableCell>
              <TableCell>{t("views.edit_system.tokens_table_purpose")}</TableCell>
              <TableCell>{t("views.edit_system.tokens_table_level")}</TableCell>
              <TableCell>{t("views.edit_system.tokens_table_created")}</TableCell>
              <TableCell>{t("views.edit_system.tokens_table_expires")}</TableCell>
              <TableCell></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {tokens.map((token) => (
              <TableRow key={token.id} sx={{opacity: isExpired(token) ? 0.5 : 1}}>
                <TableCell>{token.name}</TableCell>
                <TableCell>{purposeLabel(token.purpose, t)}</TableCell>
                <TableCell>
                  {token.purpose === TokenPurpose.VIEW ? levelLabel(token.additionalInfo, t) : "—"}
                  {token.purpose === TokenPurpose.VIEW && (
                    <Typography variant="caption" color="textSecondary" display="block">
                      {shareLinks.find(l => l.id === token.linkId)?.notes || t("views.edit_system.tokens_source_link")}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>{formatDate(token.createdAt)}</TableCell>
                <TableCell>
                  {token.expiresAt
                    ? isExpired(token)
                      ? <Chip label={t("views.edit_system.tokens_expired")} color="error" size="small"/>
                      : formatDate(token.expiresAt)
                    : t("views.edit_system.tokens_expires_never")}
                </TableCell>
                <TableCell>
                  {token.purpose !== TokenPurpose.VIEW && (
                    <IconButton onClick={() => handleOpenEdit(token)} disabled={loading} size="small">
                      <EditIcon/>
                    </IconButton>
                  )}
                  <IconButton onClick={() => handleDelete(token.id)} disabled={loading} size="small">
                    <DeleteIcon/>
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {tokens.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography variant="body2" color="textSecondary">
                    {t("views.edit_system.tokens_empty")}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Share Links */}
      <div style={{marginTop: "24px"}}>
        <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px"}}>
          <h3 style={{margin: 0, display: "flex", alignItems: "center", gap: "6px"}}>
            <LinkIcon/> {t("views.edit_system.sharelinks_header")}
          </h3>
          <Button variant="outlined" onClick={() => setShareDialogOpen(true)}>
            {t("views.edit_system.sharelinks_create")}
          </Button>
        </div>

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t("views.edit_system.sharelinks_table_notes")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_level")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_remaining")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_expires")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_token_expires")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_status")}</TableCell>
                <TableCell>{t("views.edit_system.sharelinks_table_created")}</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {shareLinks.map((link) => (
                <TableRow key={link.id}>
                  <TableCell>{link.notes || "—"}</TableCell>
                  <TableCell>{levelLabel(link.level, t)}</TableCell>
                  <TableCell>
                    {link.remainingUsages !== undefined ? link.remainingUsages : t("views.edit_system.sharelinks_unlimited")}
                  </TableCell>
                  <TableCell>
                    {link.expiresAt
                      ? isLinkExpired(link)
                        ? <Chip label={t("views.edit_system.sharelinks_expired")} color="error" size="small"/>
                        : formatDate(link.expiresAt)
                      : t("views.edit_system.sharelinks_never")}
                  </TableCell>
                  <TableCell>
                    {link.tokenExpiresAt ? formatDate(link.tokenExpiresAt) : t("views.edit_system.sharelinks_never")}
                  </TableCell>
                  <TableCell>
                    {link.revoked
                      ? <Chip label={t("views.edit_system.sharelinks_status_revoked")} color="error" size="small"/>
                      : isLinkExpired(link)
                        ? <Chip label={t("views.edit_system.sharelinks_expired")} color="error" size="small"/>
                        : <Chip label={t("views.edit_system.sharelinks_status_active")} color="success" size="small"/>}
                  </TableCell>
                  <TableCell>{formatDate(link.createdAt)}</TableCell>
                  <TableCell>
                    {!link.revoked && (
                      <IconButton onClick={() => handleRevokeShareLink(link.id)} disabled={loading} size="small" title={t("views.edit_system.sharelinks_revoke")}>
                        <BlockIcon/>
                      </IconButton>
                    )}
                    <IconButton onClick={() => setCleanupLink(link)} disabled={loading} size="small" title={t("views.edit_system.sharelinks_cleanup")}>
                      <DeleteIcon/>
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {shareLinks.length === 0 && !loading && (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    <Typography variant="body2" color="textSecondary">
                      {t("views.edit_system.sharelinks_empty")}
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </div>

      {/* Create Access Link Dialog */}
      <Dialog open={shareDialogOpen} onClose={() => setShareDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.sharelinks_create")}</DialogTitle>
        <DialogContent>
          <div style={{display: "flex", flexDirection: "column", gap: "16px", marginTop: "8px"}}>
            <FormControl fullWidth>
              <InputLabel>{t("views.edit_system.tokens_level_label")}</InputLabel>
              <Select
                value={shareLevel}
                label={t("views.edit_system.tokens_level_label")}
                onChange={(e) => setShareLevel(e.target.value as ViewTokenLevel)}
              >
                {VIEW_LEVELS.map((level) => (
                  <MenuItem key={level} value={level}>
                    {levelLabel(level, t)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label={t("views.edit_system.sharelinks_maxuses_label")}
              value={shareMaxUses}
              onChange={(e) => setShareMaxUses(e.target.value)}
              type="number"
               slotProps={{input: {min: 1}}}
               helperText={t("views.edit_system.sharelinks_maxuses_hint")}
              fullWidth
            />
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DatePicker
                label={t("views.edit_system.sharelinks_expiry_label")}
                value={shareExpiry}
                onChange={(val) => setShareExpiry(val)}
                slotProps={{textField: {fullWidth: true}}}
              />
            </LocalizationProvider>
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DatePicker
                label={t("views.edit_system.sharelinks_token_expiry_label")}
                value={shareTokenExpiry}
                onChange={(val) => setShareTokenExpiry(val)}
                slotProps={{textField: {fullWidth: true}}}
              />
            </LocalizationProvider>
            <TextField
              label={t("views.edit_system.sharelinks_notes_label")}
              value={shareNotes}
              onChange={(e) => setShareNotes(e.target.value)}
              fullWidth
              multiline
              minRows={1}
              maxRows={3}
            />
            <div style={{display: "flex", flexDirection: "column", gap: "8px"}}>
              <Typography variant="body2" color="textSecondary">
                {t("views.edit_system.sharelinks_secret_label")}
              </Typography>
              <div style={{display: "flex", gap: "16px"}}>
                <label style={{display: "flex", alignItems: "center", gap: "6px", cursor: "pointer"}}>
                  <input
                    type="radio"
                    name="shareSecretMode"
                    checked={shareSecretMode === "generate"}
                    onChange={() => setShareSecretMode("generate")}
                  />
                  {t("views.edit_system.sharelinks_secret_generate")}
                </label>
                {isAdmin && (
                  <label style={{display: "flex", alignItems: "center", gap: "6px", cursor: "pointer"}}>
                    <input
                      type="radio"
                      name="shareSecretMode"
                      checked={shareSecretMode === "manual"}
                      onChange={() => setShareSecretMode("manual")}
                    />
                    {t("views.edit_system.sharelinks_secret_manual")}
                  </label>
                )}
              </div>
              {shareSecretMode === "manual" && (
                <TextField
                  label={t("views.edit_system.sharelinks_secret_manual_label")}
                  value={shareSecret}
                  onChange={(e) => setShareSecret(e.target.value)}
                  type={shareSecretVisible ? "text" : "password"}
                  error={!!shareSecret && !VIEW_PASSWORD_PATTERN.test(shareSecret)}
                  helperText={shareSecret && !VIEW_PASSWORD_PATTERN.test(shareSecret)
                    ? t("views.edit_system.sharelinks_secret_manual_invalid")
                    : t("views.edit_system.sharelinks_secret_manual_hint")}
                  fullWidth
                  slotProps={{
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => setShareSecretVisible(v => !v)} edge="end">
                            {shareSecretVisible ? <VisibilityOffIcon/> : <VisibilityIcon/>}
                          </IconButton>
                        </InputAdornment>
                      )
                    }
                  }}
                />
              )}
            </div>
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShareDialogOpen(false)}>{t("common.cancel")}</Button>
          <Button
            onClick={handleCreateShareLink}
            disabled={loading
              || (shareMaxUses.trim() !== "" && (isNaN(parseInt(shareMaxUses, 10)) || parseInt(shareMaxUses, 10) <= 0))
              || (shareSecretMode === "manual" && !VIEW_PASSWORD_PATTERN.test(shareSecret))}
            variant="contained"
          >
            {t("common.create")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Access Link Created Dialog - shown once */}
      <Dialog open={!!createdShareLink} maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.sharelinks_created_title")}</DialogTitle>
        <DialogContent>
          <Typography color="warning.main" sx={{marginBottom: "16px"}}>
            {t("views.edit_system.sharelinks_created_message")}
          </Typography>
          <TextField
            value={createdShareLink?.secret ? buildShareLinkUrl(createdShareLink.secret) : ""}
            fullWidth
            multiline
            slotProps={{
              input: {
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => createdShareLink?.secret && handleCopyShareLink(buildShareLinkUrl(createdShareLink.secret))} edge="end">
                      <ContentCopyIcon/>
                    </IconButton>
                  </InputAdornment>
                )
              }
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreatedShareLink(null)} variant="contained">
            {t("common.close")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Cleanup Confirm Dialog */}
      <Dialog open={!!cleanupLink} onClose={() => setCleanupLink(null)} maxWidth="xs" fullWidth>
        <DialogTitle>{t("views.edit_system.sharelinks_cleanup_title")}</DialogTitle>
        <DialogContent>
          <Typography>
            {t("views.edit_system.sharelinks_cleanup_message")}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCleanupLink(null)}>{t("common.cancel")}</Button>
          <Button onClick={handleCleanupShareLink} color="error" variant="contained">
            {t("views.edit_system.sharelinks_cleanup_confirm")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Create Token Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.tokens_create")}</DialogTitle>
        <DialogContent>
          <div style={{display: "flex", flexDirection: "column", gap: "16px", marginTop: "8px"}}>
            <TextField
              label={t("views.edit_system.tokens_name_label")}
              value={newName}
              onChange={(e) => {
                setNewName(e.target.value);
                setNewNameError(validateTokenName(e.target.value));
              }}
              error={!!newNameError}
              helperText={newNameError}
              fullWidth
              autoFocus
            />
            <FormControl fullWidth>
              <InputLabel>{t("views.edit_system.tokens_purpose_label")}</InputLabel>
              <Select
                value={newPurpose}
                label={t("views.edit_system.tokens_purpose_label")}
                onChange={(e) => setNewPurpose(e.target.value as TokenPurpose)}
              >
                <MenuItem value={TokenPurpose.DATA_PUSH_REST}>
                  {t("views.edit_system.tokens_purpose_rest")}
                </MenuItem>
                <MenuItem value={TokenPurpose.DATA_PUSH_ENCRYPTED}>
                  {t("views.edit_system.tokens_purpose_encrypted")}
                </MenuItem>
              </Select>
            </FormControl>
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DatePicker
                label={t("views.edit_system.tokens_expiry_label")}
                value={newExpiry}
                onChange={(val) => setNewExpiry(val)}
                slotProps={{textField: {fullWidth: true}}}
              />
            </LocalizationProvider>
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>{t("common.cancel")}</Button>
          <Button
            onClick={handleCreate}
            disabled={!newName || !!newNameError || loading}
            variant="contained"
          >
            {t("common.create")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Token Dialog */}
      <Dialog open={!!editToken} onClose={handleCloseEdit} maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.tokens_edit_title")}</DialogTitle>
        <DialogContent>
          <div style={{display: "flex", flexDirection: "column", gap: "16px", marginTop: "8px"}}>
            <TextField
              label={t("views.edit_system.tokens_name_label")}
              value={editName}
              onChange={(e) => {
                setEditName(e.target.value);
                setEditNameError(validateTokenName(e.target.value));
              }}
              error={!!editNameError}
              helperText={editNameError}
              fullWidth
              autoFocus
            />
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DatePicker
                label={t("views.edit_system.tokens_expiry_label")}
                value={editExpiry}
                onChange={(val) => setEditExpiry(val)}
                slotProps={{textField: {fullWidth: true}}}
              />
            </LocalizationProvider>

            <Divider/>

            {regenerateConfirmStep === 0 && (
              <Button color="warning" variant="outlined" onClick={() => setRegenerateConfirmStep(1)}>
                {t("views.edit_system.tokens_regenerate")}
              </Button>
            )}
            {regenerateConfirmStep === 1 && (
              <div style={{display: "flex", flexDirection: "column", gap: "8px"}}>
                <Typography color="error">
                  {t("views.edit_system.tokens_regenerate_warning")}
                </Typography>
                <div style={{display: "flex", gap: "8px"}}>
                  <Button color="error" variant="contained" onClick={handleRegenerate} disabled={loading}>
                    {t("views.edit_system.tokens_regenerate_confirm")}
                  </Button>
                  <Button onClick={() => setRegenerateConfirmStep(0)}>
                    {t("common.cancel")}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseEdit}>{t("common.cancel")}</Button>
          <Button onClick={handleSaveEdit} disabled={!editName || !!editNameError || loading} variant="contained">
            {t("common.save")}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Token Created Dialog - shown once after creation */}
      <Dialog open={!!createdToken} maxWidth="sm" fullWidth>
        <DialogTitle>{t("views.edit_system.tokens_created_title")}</DialogTitle>
        <DialogContent>
          <Typography color="warning.main" sx={{marginBottom: "16px"}}>
            {t("views.edit_system.tokens_created_message")}
          </Typography>
          <TextField
            value={createdToken?.token || ""}
            fullWidth
            multiline
            slotProps={{
              input: {
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={handleCopyToken} edge="end">
                      <ContentCopyIcon/>
                    </IconButton>
                  </InputAdornment>
                )
              }
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreatedToken(null)} variant="contained">
            {t("views.edit_system.tokens_copied")}
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
}
