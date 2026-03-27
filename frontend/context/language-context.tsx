"use client"

import React, { createContext, useContext, useState, useEffect, ReactNode } from "react"
import { translations, Language } from "@/lib/i18n/translations"

type TranslationValues = typeof translations.en | typeof translations.fr;

interface LanguageContextType {
  language: Language
  setLanguage: (lang: Language) => void
  t: TranslationValues
  formatCurrency: (amount: number) => string
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined)

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>("fr") // Default to French for Cameroon

  useEffect(() => {
    // Load saved language preference
    const saved = localStorage.getItem("roombay-language") as Language
    if (saved && (saved === "en" || saved === "fr")) {
      setLanguageState(saved)
    } else {
      // Detect browser language
      const browserLang = navigator.language.toLowerCase()
      if (browserLang.startsWith("fr")) {
        setLanguageState("fr")
      } else {
        setLanguageState("en")
      }
    }
  }, [])

  const setLanguage = (lang: Language) => {
    setLanguageState(lang)
    localStorage.setItem("roombay-language", lang)
  }

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat(language === "fr" ? "fr-CM" : "en-CM", {
      style: "decimal",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount) + " FCFA"
  }

  const value: LanguageContextType = {
    language,
    setLanguage,
    t: translations[language],
    formatCurrency,
  }

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  const context = useContext(LanguageContext)
  if (context === undefined) {
    throw new Error("useLanguage must be used within a LanguageProvider")
  }
  return context
}
