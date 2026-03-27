"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"

export default function ListingsRedirect() {
  const router = useRouter()
  
  useEffect(() => {
    router.replace("/search?mode=forYou&view=reels")
  }, [router])
  
  return null
}
