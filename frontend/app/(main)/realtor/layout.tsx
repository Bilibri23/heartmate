import type { ReactNode } from "react";
import { RoleGuard } from "@/components/auth/role-guard";

export default function RealtorLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={["REALTOR"]}>{children}</RoleGuard>;
}
