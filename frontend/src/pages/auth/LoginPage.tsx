import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion } from 'framer-motion';
import { Eye, EyeOff, Lock, Mail, User as UserIcon } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { UserRole } from '@/types';
import { ROLE_HOME, ROLE_LABELS } from '@/constants';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/forms/FormFields';
import toast from 'react-hot-toast';

const authSchema = z.object({
  name: z.string().optional(),
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  role: z.nativeEnum(UserRole),
});

type AuthFormData = z.infer<typeof authSchema>;

const roleOptions = [
  { value: UserRole.STUDENT, label: 'Student' },
  { value: UserRole.FACULTY, label: 'Faculty' },
  { value: UserRole.HOD, label: 'HOD' },
  { value: UserRole.ADMIN, label: 'Admin' },
];

// Demo credentials per role
const DEMO_CREDENTIALS: Record<UserRole, { email: string; password: string }> = {
  [UserRole.STUDENT]: { email: 'arjun.sharma@secureiq.edu', password: 'student123' },
  [UserRole.FACULTY]: { email: 'meera.krishnan@secureiq.edu', password: 'faculty123' },
  [UserRole.HOD]: { email: 'suresh.babu@secureiq.edu', password: 'hod12345' },
  [UserRole.ADMIN]: { email: 'admin@secureiq.edu', password: 'admin123' },
};

export function LoginPage() {
  const { login, registerUser } = useAuth();
  const navigate = useNavigate();
  const [showPass, setShowPass] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const [selectedRole, setSelectedRole] = useState<UserRole>(UserRole.STUDENT);

  const { register, handleSubmit, setValue, getValues, formState: { errors, isSubmitting }, reset } = useForm<AuthFormData>({
    resolver: zodResolver(authSchema),
    defaultValues: { role: UserRole.STUDENT },
  });

  const handleRoleSelect = (role: UserRole) => {
    setSelectedRole(role);
    setValue('role', role);
    if (!isRegister) {
      const currentEmail = getValues('email');
      const isDemoEmail = Object.values(DEMO_CREDENTIALS).some(c => c.email === currentEmail);
      if (!currentEmail || currentEmail.trim() === '' || isDemoEmail) {
        const creds = DEMO_CREDENTIALS[role];
        setValue('email', creds.email);
        setValue('password', creds.password);
      }
    }
  };

  const onSubmit = async (data: AuthFormData) => {
    try {
      if (isRegister) {
        if (!data.name || data.name.trim() === '') {
          toast.error('Name is required for registration');
          return;
        }
        const user = await registerUser({
          name: data.name,
          email: data.email,
          password: data.password,
          role: data.role,
        });
        if (user) {
          toast.success(`Account registered successfully as ${ROLE_LABELS[user.role]}!`);
          navigate(ROLE_HOME[user.role]);
        }
      } else {
        const user = await login({
          email: data.email,
          password: data.password,
          role: data.role,
        });
        if (user) {
          toast.success(`Welcome back! Logged in as ${ROLE_LABELS[user.role]}`);
          navigate(ROLE_HOME[user.role]);
        }
      }
    } catch (err: any) {
      const errMsg = err.response?.data?.message || 'Authentication failed. Please check credentials.';
      toast.error(errMsg);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="space-y-6"
    >
      <div>
        <h2 className="font-display font-bold text-foreground text-2xl">
          {isRegister ? 'Create an account' : 'Welcome back'}
        </h2>
        <p className="text-muted-foreground text-sm mt-1">
          {isRegister ? 'Register your SecureIQ credentials' : 'Sign in to your SecureIQ account'}
        </p>
      </div>

      {/* Role selector */}
      <div className="space-y-2">
        <label className="text-sm font-medium text-foreground">Select Role</label>
        <div className="grid grid-cols-4 gap-2">
          {roleOptions.map((role) => (
            <button
              key={role.value}
              type="button"
              onClick={() => handleRoleSelect(role.value)}
              className={`
                relative flex items-center justify-center py-2.5 rounded-lg border text-center text-xs font-semibold transition-all duration-200
                ${selectedRole === role.value
                  ? 'border-primary bg-primary/5 text-primary shadow-glow-sm'
                  : 'border-border bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground'
                }
              `}
            >
              {role.label}
              {selectedRole === role.value && (
                <span className="absolute top-1 right-1 h-1.5 w-1.5 rounded-full bg-primary animate-pulse" />
              )}
            </button>
          ))}
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {isRegister && (
          <Input
            label="Full Name"
            placeholder="John Doe"
            leftIcon={<UserIcon className="h-4 w-4" />}
            error={errors.name?.message}
            required
            {...register('name')}
          />
        )}

        <Input
          label="Email Address"
          type="email"
          placeholder="you@institution.edu"
          leftIcon={<Mail className="h-4 w-4" />}
          error={errors.email?.message}
          required
          {...register('email')}
        />

        <Input
          label="Password"
          type={showPass ? 'text' : 'password'}
          placeholder="Enter your password"
          leftIcon={<Lock className="h-4 w-4" />}
          rightIcon={
            <button type="button" onClick={() => setShowPass((p) => !p)} className="hover:text-foreground transition-colors">
              {showPass ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.password?.message}
          required
          {...register('password')}
        />

        {!isRegister && (
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-sm text-muted-foreground cursor-pointer">
              <input type="checkbox" className="rounded border-border" />
              Remember me
            </label>
            <button type="button" className="text-sm text-primary hover:underline">
              Forgot password?
            </button>
          </div>
        )}

        <Button type="submit" variant="primary" size="lg" className="w-full" isLoading={isSubmitting}>
          {isRegister ? 'Sign Up' : 'Sign In'}
        </Button>
      </form>

      {/* Switch mode */}
      <div className="text-center text-sm text-muted-foreground mt-4">
        {isRegister ? (
          <>
            Already have an account?{' '}
            <button
              type="button"
              onClick={() => { setIsRegister(false); reset(); setSelectedRole(UserRole.STUDENT); }}
              className="text-primary font-semibold hover:underline focus:outline-none"
            >
              Sign In
            </button>
          </>
        ) : (
          <>
            New to SecureIQ?{' '}
            <button
              type="button"
              onClick={() => { setIsRegister(true); reset(); setSelectedRole(UserRole.STUDENT); }}
              className="text-primary font-semibold hover:underline focus:outline-none"
            >
              Create an account
            </button>
          </>
        )}
      </div>

      {/* Demo notice (only shown in login mode to prevent noise) */}
      {!isRegister && (
        <div className="rounded-lg border border-border bg-muted/40 p-3">
          <p className="text-xs text-muted-foreground text-center">
            <span className="font-semibold text-foreground">Demo Mode</span> — Click a role above to auto-fill credentials
          </p>
        </div>
      )}
    </motion.div>
  );
}
