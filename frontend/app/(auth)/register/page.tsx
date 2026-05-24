"use client"

import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Loader2, ArrowLeft, Search, Building2, Eye, EyeOff } from "lucide-react"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useState, Suspense } from "react"
import { countries, getDefaultCountry, type Country } from "@/lib/countries"
import { startGoogleOAuthWithSignupRole } from "@/lib/oauth-signup-role-cookie"

const registerSchema = z.object({
  firstName: z.string().min(2, { message: "First name must be at least 2 characters" }),
  lastName: z.string().min(2, { message: "Last name must be at least 2 characters" }),
  email: z.string().email({ message: "Please enter a valid email address" }),
  countryCode: z.string().min(1, { message: "Please select a country" }),
  phone: z.string().min(8, { message: "Phone number is required" }),
  gender: z.enum(["MALE", "FEMALE"], { message: "Please select your gender" }),
  password: z.string().min(8, { message: "Password must be at least 8 characters" }),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
})

type RegisterFormValues = z.infer<typeof registerSchema>

function RegisterContent() {
  const searchParams = useSearchParams()
  const roleFromUrl = searchParams.get("role") as "TENANT" | "LANDLORD" | null
  const { register: registerUser } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [selectedRole, setSelectedRole] = useState<"TENANT" | "LANDLORD">(roleFromUrl || "TENANT")
  const defaultCountry = getDefaultCountry()
  const [selectedCountry, setSelectedCountry] = useState<Country>(defaultCountry)
  // Default to visible so local testing doesn't silently hide OAuth.
  const showGoogleOAuth = process.env.NEXT_PUBLIC_OAUTH_GOOGLE_ENABLED !== "false"
  // Same-origin OAuth so localhost stays on localhost and ngrok stays on ngrok (avoids .env pointing at a dead tunnel).
  const signupRoleParam =
    selectedRole === "LANDLORD" ? "LANDLORD" : "STUDENT"
  const googleOAuthUrl = `${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/google?signup_role=${encodeURIComponent(signupRoleParam)}`

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      countryCode: getDefaultCountry().dialCode, // Store dial code
      phone: "",
      gender: undefined,
      password: "",
      confirmPassword: "",
    },
  })

  async function onSubmit(data: RegisterFormValues) {
    setIsLoading(true)
    setError(null)
    try {
      // Combine country code with phone number
      const fullPhone = `${data.countryCode}${data.phone.replace(/^\+/, '')}`
      
      await registerUser({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        phone: fullPhone,
        password: data.password,
        role: selectedRole === "TENANT" ? "STUDENT" : selectedRole,
        gender: data.gender,
      })
    } catch (err: any) {
      setError(err.response?.data?.message || "Registration failed. Please try again.")
    } finally {
      setIsLoading(false)
    }
  }

  const isTenant = selectedRole === "TENANT"

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
            <div className={`mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl ${isTenant ? 'bg-blue-50 text-blue-600' : 'bg-emerald-50 text-emerald-600'}`}>
              {isTenant ? <Search className="h-7 w-7" /> : <Building2 className="h-7 w-7" />}
            </div>
            <h1 className="text-2xl font-bold text-slate-900">Create your account</h1>
            <p className="mt-2 text-sm text-slate-500">
              {isTenant ? "Find your perfect home" : "List your properties"}
            </p>
          </div>

          {/* Role Toggle */}
          <div className="mb-6 flex rounded-xl bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => setSelectedRole("TENANT")}
              className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition-all ${
                isTenant 
                  ? 'bg-white text-slate-900 shadow-sm' 
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Tenant
            </button>
            <button
              type="button"
              onClick={() => setSelectedRole("LANDLORD")}
              className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition-all ${
                !isTenant 
                  ? 'bg-white text-slate-900 shadow-sm' 
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Landlord
            </button>
          </div>

          {showGoogleOAuth && (
            <div className="mb-6">
              <Button
                type="button"
                variant="outline"
                className="h-11 w-full rounded-xl border-slate-200"
                onClick={() =>
                  startGoogleOAuthWithSignupRole(
                    googleOAuthUrl,
                    signupRoleParam === "LANDLORD" ? "LANDLORD" : "STUDENT"
                  )
                }
              >
                Sign up with Google
              </Button>
              <div className="relative my-5 text-center text-xs text-slate-400">
                <span className="bg-white px-2">or register with email</span>
                <div className="absolute inset-x-0 top-1/2 -z-10 h-px bg-slate-200" aria-hidden />
              </div>
            </div>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="firstName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-slate-700">First Name</FormLabel>
                      <FormControl>
                        <Input 
                          placeholder="John" 
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
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-slate-700">Last Name</FormLabel>
                      <FormControl>
                        <Input 
                          placeholder="Doe" 
                          className="h-11 rounded-xl border-slate-200 bg-slate-50 focus:bg-white" 
                          {...field} 
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
              
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-slate-700">Email</FormLabel>
                    <FormControl>
                      <Input 
                        type="email"
                        placeholder="you@example.com" 
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
                name="countryCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-slate-700">Country</FormLabel>
                    <Select 
                      value={selectedCountry.code}
                      onValueChange={(value) => {
                        const country = countries.find(c => c.code === value)
                        if (country) {
                          setSelectedCountry(country)
                          field.onChange(country.dialCode) // Store dial code in form
                        }
                      }}
                    >
                      <FormControl>
                        <SelectTrigger className="h-11 rounded-xl border-slate-200 bg-slate-50">
                          <SelectValue placeholder="Select country" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent className="max-h-[300px]">
                        {countries.map((country) => (
                          <SelectItem key={`country-${country.code}`} value={country.code}>
                            <span className="flex items-center gap-2">
                              <span>{country.flag}</span>
                              <span>{country.name}</span>
                              <span className="text-slate-500 ml-auto">{country.dialCode}</span>
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="phone"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-slate-700">Phone Number</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-sm">
                            {selectedCountry.dialCode}
                          </span>
                          <Input 
                            placeholder="683068251" 
                            className="h-11 rounded-xl border-slate-200 bg-slate-50 pl-20 focus:bg-white" 
                            {...field}
                            onChange={(e) => {
                              // Remove any non-digit characters
                              const digits = e.target.value.replace(/\D/g, '')
                              field.onChange(digits)
                            }}
                          />
                        </div>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="gender"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-slate-700">Gender</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger className="h-11 rounded-xl border-slate-200 bg-slate-50">
                            <SelectValue placeholder="Select" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="MALE">Male</SelectItem>
                          <SelectItem value="FEMALE">Female</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-slate-700">Password</FormLabel>
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

              <FormField
                control={form.control}
                name="confirmPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-slate-700">Confirm Password</FormLabel>
                    <FormControl>
                      <Input 
                        type="password"
                        placeholder="••••••••" 
                        className="h-11 rounded-xl border-slate-200 bg-slate-50 focus:bg-white" 
                        {...field} 
                      />
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
                className={`h-11 w-full rounded-xl text-sm font-medium ${
                  isTenant
                    ? 'bg-slate-900 hover:bg-slate-800'
                    : 'bg-emerald-600 hover:bg-emerald-700'
                }`}
                disabled={isLoading}
              >
                <span className="inline-flex h-4 w-4 shrink-0 items-center justify-center" aria-hidden={!isLoading}>
                  {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                </span>
                <span>Create Account</span>
              </Button>
            </form>
          </Form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Already have an account?{" "}
            <Link href="/login" className="font-medium text-slate-900 hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default function RegisterPage() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
      </div>
    }>
      <RegisterContent />
    </Suspense>
  )
}
