"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { ProfileCompletionStatus } from "@/services/profile-completion";

const LABELS: Record<string, string> = {
  CONTACT_VERIFIED: "Verify email or phone (either is enough)",
  EMAIL_VERIFIED: "Verify your email",
  PHONE_VERIFIED: "Verify your phone",
  PROFILE_BASICS: "Finish your public profile (photo & basic details)",
  PREFERENCES: "Set roommate preferences",
  IDENTITY_VERIFICATION: "Complete identity verification",
  BUSINESS_VERIFICATION: "Business verification (optional)",
  PROPERTY_DOCS: "Upload property ownership proof",
};

function continueHref(status: ProfileCompletionStatus) {
  if (status.missingSteps.includes("CONTACT_VERIFIED")) {
    return "/account/verify";
  }
  if (status.role === "LANDLORD") return "/landlord/profile";
  if (status.role === "STUDENT") return "/verification";
  return "/settings";
}

export function CompletionBanner({ status }: { status: ProfileCompletionStatus }) {
  if (!status.missingSteps.length) return null;

  const href = continueHref(status);

  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
      <div className="flex items-start gap-2">
        <AlertTriangle className="h-4 w-4 text-amber-700 mt-0.5" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-amber-800">
            Profile completion: {status.completionPercentage}%
          </p>
          <p className="text-xs text-amber-700 mt-1 leading-relaxed">
            Still to do: {status.missingSteps.map((s) => LABELS[s] || s.replaceAll("_", " ")).join(" · ")}
          </p>
          <p className="text-[11px] text-amber-700/90 mt-1.5 leading-snug">
            {status.role === "LANDLORD"
              ? "We only need one verified contact method (email or phone) — not both."
              : "Verify email or phone; then finish the steps that apply to you."}
          </p>
          <div className="mt-2 flex flex-col gap-1.5 sm:flex-row sm:items-center sm:gap-3">
            <Link href={href} className="text-xs font-medium text-amber-900 underline inline-block">
              {status.missingSteps.includes("CONTACT_VERIFIED") ? "Verify email or phone" : "Continue onboarding"}
            </Link>
            {status.missingSteps.includes("CONTACT_VERIFIED") && (
              <Link
                href={status.role === "LANDLORD" ? "/landlord/profile" : "/profile"}
                className="text-[11px] text-amber-800/90 underline"
              >
                Profile hub
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
