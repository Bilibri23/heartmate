"use client";

import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { ProfileCompletionStatus } from "@/services/profile-completion";

const LABELS: Record<string, string> = {
  EMAIL_VERIFIED: "Verify your email",
  PHONE_VERIFIED: "Verify your phone",
  PROFILE_BASICS: "Complete profile basics",
  PREFERENCES: "Set roommate preferences",
  IDENTITY_VERIFICATION: "Complete identity verification",
  BUSINESS_VERIFICATION: "Complete business verification",
  PROPERTY_DOCS: "Upload property documents",
  PAYOUT_DETAILS: "Add payout details",
};

export function CompletionBanner({ status }: { status: ProfileCompletionStatus }) {
  if (!status.missingSteps.length) return null;

  return (
    <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
      <div className="flex items-start gap-2">
        <AlertTriangle className="h-4 w-4 text-amber-700 mt-0.5" />
        <div className="flex-1">
          <p className="text-sm font-semibold text-amber-800">
            Profile completion: {status.completionPercentage}%
          </p>
          <p className="text-xs text-amber-700 mt-1">
            Missing: {status.missingSteps.map((s) => LABELS[s] || s).join(", ")}
          </p>
          <Link href="/verification" className="text-xs text-amber-800 underline mt-1 inline-block">
            Continue verification and onboarding
          </Link>
        </div>
      </div>
    </div>
  );
}
