"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html lang="en">
      <body className="p-6">
        <h2 className="text-xl font-semibold mb-2">Something went wrong</h2>
        <p className="text-sm text-slate-600 mb-4">
          We have logged this error for investigation.
        </p>
        <button
          type="button"
          onClick={() => reset()}
          className="rounded bg-blue-600 px-4 py-2 text-white"
        >
          Try again
        </button>
      </body>
    </html>
  );
}
