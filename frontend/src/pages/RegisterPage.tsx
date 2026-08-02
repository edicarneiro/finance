import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ApiError } from '../api/httpClient'
import { useAuth } from '../auth/useAuth'
import styles from './AuthForm.module.css'

// Validação de forma apenas — a regra de negócio (PasswordPolicy) é do backend, nunca duplicada aqui (ver ADR-0025).
const registerSchema = z
  .object({
    email: z.string().min(1, 'Informe o e-mail.').email('E-mail inválido.'),
    password: z.string().min(8, 'A senha deve ter ao menos 8 caracteres.'),
    confirmPassword: z.string().min(1, 'Confirme a senha.'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'As senhas não coincidem.',
    path: ['confirmPassword'],
  })

type RegisterFormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [submitError, setSubmitError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) })

  const onSubmit = async (values: RegisterFormValues) => {
    setSubmitError(null)
    try {
      await registerUser(values.email, values.password)
      navigate('/', { replace: true })
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.message : 'Não foi possível criar a conta. Tente novamente.')
    }
  }

  return (
    <div className={styles.container}>
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        <h1>Criar conta</h1>

        <label htmlFor="email">E-mail</label>
        <input id="email" type="email" autoComplete="email" {...register('email')} />
        {errors.email && <p role="alert">{errors.email.message}</p>}

        <label htmlFor="password">Senha</label>
        <input id="password" type="password" autoComplete="new-password" {...register('password')} />
        {errors.password && <p role="alert">{errors.password.message}</p>}

        <label htmlFor="confirmPassword">Confirmar senha</label>
        <input id="confirmPassword" type="password" autoComplete="new-password" {...register('confirmPassword')} />
        {errors.confirmPassword && <p role="alert">{errors.confirmPassword.message}</p>}

        {submitError && <p role="alert">{submitError}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando conta…' : 'Criar conta'}
        </button>

        <p>
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  )
}
