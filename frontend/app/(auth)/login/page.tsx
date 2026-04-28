"use client"

import Link from "next/link"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Loader2, ArrowLeft, Home, Eye, EyeOff } from "lucide-react"
import { useAuth } from "@/context/auth-context"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useEffect, useState } from "react"
import { clearOAuthSignupRoleCookie } from "@/lib/oauth-signup-role-cookie"

const loginSchema = z.object({
  emailOrPhone: z.string().min(1, { message: "Email or phone is required" }),
  password: z.string().min(1, { message: "Password is required" }),
})

type LoginFormValues = z.infer<typeof loginSchema>

export default function LoginPage() {
  const { login } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  // Default to visible so local testing doesn't silently hide OAuth.
  const showGoogleOAuth = process.env.NEXT_PUBLIC_OAUTH_GOOGLE_ENABLED !== "false"
  // Same-origin OAuth so localhost stays on localhost and ngrok stays on ngrok.
  const googleOAuthUrl = "/oauth2/authorization/google"

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      emailOrPhone: "",
      password: "",
    },
  })

  useEffect(() => {
    clearOAuthSignupRoleCookie()
  }, [])

  async function onSubmit(data: LoginFormValues) {
    setIsLoading(true)
    setError(null)
    try {
      await login({
        emailOrPhone: data.emailOrPhone,
        password: data.password,
      })
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } }
      const status = ax.response?.status
      const apiMessage = ax.response?.data?.message
      if (apiMessage) {
        setError(apiMessage)
      } else if (status === 503 || status === 502) {
        setError("The server could not reach the database. Start PostgreSQL and confirm DATABASE_URL matches your machine.")
      } else if (status === 500) {
        setError("Server error during sign-in. Check the Spring Boot console for the stack trace.")
      } else {
        setError("Invalid credentials. Please try again.")
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 p-6">
      <div className="w-full max-w-md">
        <Link 
          href="/" 
          className="mb-8 inline-flex items-center gap-2 text-sm text-slate-600 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to home
        </Link>

        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
              <Home className="h-7 w-7" />
            </div>
            <h1 className="text-2xl font-bold text-slate-900">Welcome back</h1>
            <p className="mt-2 text-sm text-slate-500">
              Sign in to your RoomBay account
            </p>
          </div>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="emailOrPhone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-slate-700">Email or Phone</FormLabel>
                    <FormControl>
                      <Input 
                        placeholder="you@example.com or +237..." 
                        className="h-11 rounded-xl border-slate-200 bg-slate-50 focus:bg-white" 
                        {...field} 
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <div className="flex items-center justify-between">
                      <FormLabel className="text-slate-700">Password</FormLabel>
                      <Link 
                        href="/forgot-password" 
                        className="text-xs text-slate-500 hover:text-slate-900"
                      >
                        Forgot password?
                      </Link>
                    </div>
                    <FormControl>
                      <div className="relative">
                        <Input 
                          type={showPassword ? "text" : "password"}
                          placeholder="••••••••" 
                          className="h-11 rounded-xl border-slate-200 bg-slate-50 pr-10 focus:bg-white" 
                          {...field} 
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {error && (
                <div className="rounded-lg bg-red-50 p-3 text-sm text-red-600">
                  {error}
                </div>
              )}

              <Button
                type="submit"
                className="h-11 w-full rounded-xl bg-slate-900 text-sm font-medium hover:bg-slate-800"
                disabled={isLoading}
              >
                {/* Stable two-node layout avoids insertBefore DOM errors when toggling the spinner (Radix Button + React 19). */}
                <span className="inline-flex h-4 w-4 shrink-0 items-center justify-center" aria-hidden={!isLoading}>
                  {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                </span>
                <span>Sign In</span>
              </Button>

              {showGoogleOAuth && (
                <>
                  <div className="relative my-6 text-center text-xs text-slate-400">
                    <span className="bg-white px-2">or</span>
                    <div className="absolute inset-x-0 top-1/2 -z-10 h-px bg-slate-200" aria-hidden />
                  </div>
                  <Button type="button" variant="outline" className="h-11 w-full rounded-xl border-slate-200" asChild>
                    <a href={googleOAuthUrl}>Continue with Google</a>
                  </Button>
                </>
              )}
            </form>
          </Form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Don&apos;t have an account?{" "}
            <Link href="/register" className="font-medium text-slate-900 hover:underline">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
