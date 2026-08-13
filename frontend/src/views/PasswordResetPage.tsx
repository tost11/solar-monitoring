import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import Button from "@mui/material/Button";
import Input from "@mui/material/Input";
import Typography from "@mui/material/Typography";
import CircularProgress from "@mui/material/CircularProgress";
import Alert from "@mui/material/Alert";
import { validatePasswordResetToken, postPasswordResetConfirm } from '../api/UserAPIFunctions';
import { useTranslation } from 'react-i18next';

export default function PasswordResetPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(true);
  const [valid, setValid] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setValid(false);
      setLoading(false);
      return;
    }

    validatePasswordResetToken(token).then(result => {
      setValid(result.valid);
      setLoading(false);
    }).catch(() => {
      setValid(false);
      setLoading(false);
    });
  }, [token]);

  useEffect(() => {
    const validatePassword = () => {
      if (!newPassword) {
        setValidationError(null);
        return;
      }

      if (!newPassword.match(
        /^(?=.*[A-ZÄÜÖ])(?=.*[a-zäöüß])(?=.*\d)(?=.*[@$%*#?!&])[A-ZÄÖÜa-zäöüß\d@$!%*#?&]{10,64}$/
      )) {
        setValidationError(t("views.registration.invalid_password_1"));
        return;
      }

      if (confirmPassword && confirmPassword !== newPassword) {
        setValidationError(t("views.registration.invalid_password_2"));
        return;
      }

      setValidationError(null);
    };

    validatePassword();
  }, [newPassword, confirmPassword, t]);

  const handleSubmit = () => {
    setError('');

    if (validationError) {
      setError(validationError);
      return;
    }

    postPasswordResetConfirm({ token, newPassword }).then(() => {
      setSuccess(true);
      setTimeout(() => navigate('/'), 3000);
    }).catch(err => {
      setError(err.response?.data?.message || 'Failed to reset password');
    });
  };

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh'
      }}>
        <CircularProgress />
      </div>
    );
  }

  if (!valid) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        padding: '20px'
      }}>
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
          padding: '40px',
          backgroundColor: 'white',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
          maxWidth: '400px',
          width: '100%',
          textAlign: 'center'
        }}>
          <Typography variant="h5">Invalid or Expired Reset Link</Typography>
          <Typography>Please request a new password reset.</Typography>
          <Button variant="contained" onClick={() => navigate('/')} fullWidth>Back to Login</Button>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        padding: '20px'
      }}>
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
          padding: '40px',
          backgroundColor: 'white',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
          maxWidth: '400px',
          width: '100%',
          textAlign: 'center'
        }}>
          <Typography variant="h5">Password Reset Successful</Typography>
          <Typography>Redirecting to login...</Typography>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      padding: '20px'
    }}>
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
        padding: '40px',
        backgroundColor: 'white',
        borderRadius: '8px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        maxWidth: '400px',
        width: '100%'
      }}>
        <Typography variant="h4" style={{textAlign: 'center'}}>Reset Your Password</Typography>

        {validationError && <Alert severity="error">{validationError}</Alert>}

        <Input
          type="password"
          placeholder="New Password (min 10 characters)"
          value={newPassword}
          onChange={e => setNewPassword(e.target.value)}
          fullWidth
        />

        <Input
          type="password"
          placeholder="Confirm Password"
          value={confirmPassword}
          onChange={e => setConfirmPassword(e.target.value)}
          fullWidth
        />

        {error && <Typography color="error" style={{textAlign: 'center'}}>{error}</Typography>}

        <Button
          variant="contained"
          disabled={!newPassword || !confirmPassword || validationError !== null}
          onClick={handleSubmit}
          fullWidth
        >
          Reset Password
        </Button>
      </div>
    </div>
  );
}
