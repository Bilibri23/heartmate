import type { ReactNode } from "react";
import { RoleGuard } from "@/components/auth/role-guard";

export default function LandlordLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={["LANDLORD"]}>{children}</RoleGuard>;
}
