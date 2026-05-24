"use client";

export default function MessagesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // The messages/page.tsx renders the full mobile-first inbox list.
  // The messages/[userId]/page.tsx renders the individual chat view.
  // Both are fully self-contained — no shared sidebar layout needed.
  return <>{children}</>;
}
