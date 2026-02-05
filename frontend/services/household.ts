import api from "@/lib/api";

// Types
export interface HouseholdMember {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  photoUrl?: string;
  role: "ADMIN" | "MEMBER";
  rentSharePercentage: number;
  joinedAt: string;
  inviteStatus: "PENDING" | "ACCEPTED" | "DECLINED";
}

export interface Household {
  id: string;
  name: string;
  description?: string;
  status: "ACTIVE" | "INACTIVE" | "ARCHIVED";
  leaseId?: string;
  listingId?: string;
  listingTitle?: string;
  listingAddress?: string;
  members: HouseholdMember[];
  createdAt: string;
  totalExpensesThisMonth: number;
  myBalanceOwed: number;
  myBalanceOwedToMe: number;
  openTasksCount: number;
  openIssuesCount: number;
}

export interface ExpenseSplit {
  id: string;
  userId: string;
  userName: string;
  userPhoto?: string;
  amount: number;
  isPaid: boolean;
  paidAt?: string;
}

export interface Expense {
  id: string;
  householdId: string;
  amount: number;
  category: ExpenseCategory;
  description: string;
  receiptUrl?: string;
  expenseDate: string;
  splitType: SplitType;
  isRecurring: boolean;
  recurringDay?: number;
  createdAt: string;
  paidById: string;
  paidByName: string;
  paidByPhoto?: string;
  splits: ExpenseSplit[];
}

export type ExpenseCategory =
  | "RENT"
  | "UTILITIES"
  | "GROCERIES"
  | "SUPPLIES"
  | "INTERNET"
  | "CLEANING"
  | "WATER"
  | "ELECTRICITY"
  | "GAS"
  | "OTHER";

export type SplitType = "EQUAL" | "PERCENTAGE" | "FIXED" | "CUSTOM";

export interface BalanceSummary {
  householdId: string;
  userId: string;
  totalPaidByMe: number;
  totalOwedByMe: number;
  totalOwedToMe: number;
  netBalance: number;
  memberBalances: MemberBalance[];
}

export interface MemberBalance {
  userId: string;
  userName: string;
  userPhoto?: string;
  amountOwed: number;
}

export interface HouseRules {
  id: string;
  householdId: string;
  quietHoursEnabled: boolean;
  quietHoursStart?: string;
  quietHoursEnd?: string;
  guestPolicy: GuestPolicy;
  cleaningSchedule: CleaningSchedule;
  smokingAllowed: boolean;
  smokingArea?: string;
  drinkingAllowed: boolean;
  petsAllowed: boolean;
  petRules?: string;
  kitchenRules?: string;
  bathroomRules?: string;
  livingRoomRules?: string;
  customRules: string[];
  agreements: RuleAgreement[];
  allMembersAgreed: boolean;
  currentUserAgreed: boolean;
  createdAt: string;
  updatedAt: string;
  lastUpdatedByName?: string;
}

export type GuestPolicy =
  | "ALWAYS_OK"
  | "NOTIFY"
  | "ASK_FIRST"
  | "NO_OVERNIGHT"
  | "NO_GUESTS";

export type CleaningSchedule = "ROTATING" | "ASSIGNED" | "FLEXIBLE" | "SHARED";

export interface RuleAgreement {
  userId: string;
  userName: string;
  userPhoto?: string;
  hasAgreed: boolean;
  agreedAt?: string;
}

export interface BillReminder {
  id: string;
  householdId: string;
  name: string;
  category: ExpenseCategory;
  amount?: number;
  isRecurring: boolean;
  dueDay?: number;
  reminderDaysBefore: number;
  isActive: boolean;
  daysUntilDue: number;
  isPastDue: boolean;
  createdById: string;
  createdByName: string;
  createdAt: string;
}

export interface Task {
  id: string;
  householdId: string;
  title: string;
  description?: string;
  category: TaskCategory;
  assignmentType: AssignmentType;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  dueTime?: string;
  isOverdue: boolean;
  isRecurring: boolean;
  recurrencePattern?: RecurrencePattern;
  recurrenceDay?: number;
  createdById: string;
  createdByName: string;
  assignedToId?: string;
  assignedToName?: string;
  assignedToPhoto?: string;
  completedById?: string;
  completedByName?: string;
  completedAt?: string;
  createdAt: string;
}

export type TaskCategory =
  | "CLEANING"
  | "SHOPPING"
  | "MAINTENANCE"
  | "COOKING"
  | "LAUNDRY"
  | "TRASH"
  | "OTHER";

export type AssignmentType = "VOLUNTARY" | "ROTATING" | "ASSIGNED";
export type TaskStatus =
  | "PENDING"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "OVERDUE"
  | "CANCELLED";
export type TaskPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export type RecurrencePattern = "DAILY" | "WEEKLY" | "BIWEEKLY" | "MONTHLY";

export interface Issue {
  id: string;
  householdId: string;
  title: string;
  description: string;
  category: IssueCategory;
  severity: IssueSeverity;
  status: IssueStatus;
  reportedById?: string;
  reportedByName: string;
  isAnonymous: boolean;
  reportedAgainstId?: string;
  reportedAgainstName?: string;
  resolution?: string;
  resolvedAt?: string;
  resolvedById?: string;
  resolvedByName?: string;
  isEscalated: boolean;
  escalatedAt?: string;
  escalatedTo?: string;
  comments: IssueComment[];
  commentCount: number;
  createdAt: string;
}

export type IssueCategory =
  | "NOISE"
  | "CLEANLINESS"
  | "BILLS"
  | "GUESTS"
  | "BEHAVIOR"
  | "PROPERTY"
  | "RULES_VIOLATION"
  | "OTHER";

export type IssueSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type IssueStatus =
  | "OPEN"
  | "ACKNOWLEDGED"
  | "IN_DISCUSSION"
  | "RESOLVED"
  | "ESCALATED"
  | "CLOSED";

export interface IssueComment {
  id: string;
  userId: string;
  userName: string;
  userPhoto?: string;
  content: string;
  isResolutionProposal: boolean;
  createdAt: string;
}

// API Service
export const householdService = {
  // Household
  async getMyHouseholds(): Promise<Household[]> {
    const response = await api.get("/households");
    return response.data;
  },

  async getHousehold(id: string): Promise<Household> {
    const response = await api.get(`/households/${id}`);
    return response.data;
  },

  async deleteHousehold(id: string): Promise<void> {
    await api.delete(`/households/${id}`);
  },

  // Households are created automatically when a shared lease becomes active
  // This internal method is kept for admin/system use only
  async createFromLease(leaseId: string): Promise<Household> {
    const response = await api.post(`/households/from-lease/${leaseId}`);
    return response.data;
  },

  async inviteMember(
    householdId: string,
    userId: string,
    rentShare?: number
  ): Promise<void> {
    await api.post(`/households/${householdId}/invite`, { userId, rentShare });
  },

  async respondToInvitation(
    householdId: string,
    accept: boolean
  ): Promise<void> {
    await api.post(`/households/${householdId}/invitation/respond`, { accept });
  },

  async leaveHousehold(householdId: string): Promise<void> {
    await api.post(`/households/${householdId}/leave`);
  },

  // Expenses
  async getExpenses(
    householdId: string,
    page = 0,
    size = 20
  ): Promise<{ content: Expense[]; totalPages: number }> {
    const response = await api.get(
      `/households/${householdId}/expenses?page=${page}&size=${size}`
    );
    return response.data;
  },

  async createExpense(
    householdId: string,
    data: {
      amount: number;
      category: ExpenseCategory;
      description: string;
      receiptUrl?: string;
      expenseDate?: string;
      splitType: SplitType;
      customSplits?: Record<string, number>;
      isRecurring?: boolean;
      recurringDay?: number;
    }
  ): Promise<Expense> {
    const response = await api.post(`/households/${householdId}/expenses`, data);
    return response.data;
  },

  async markSplitAsPaid(splitId: string): Promise<void> {
    await api.post(`/households/expenses/splits/${splitId}/mark-paid`);
  },

  async recordSettlement(
    householdId: string,
    data: {
      toUserId: string;
      amount: number;
      note?: string;
      settlementMethod?: "CASH" | "MOBILE_MONEY" | "BANK_TRANSFER" | "OTHER";
    }
  ): Promise<void> {
    await api.post(`/households/${householdId}/settlements`, data);
  },

  async getBalanceSummary(householdId: string): Promise<BalanceSummary> {
    const response = await api.get(`/households/${householdId}/balance`);
    return response.data;
  },

  // House Rules
  async getHouseRules(householdId: string): Promise<HouseRules> {
    const response = await api.get(`/households/${householdId}/rules`);
    return response.data;
  },

  async updateHouseRules(
    householdId: string,
    data: Partial<HouseRules>
  ): Promise<HouseRules> {
    const response = await api.put(`/households/${householdId}/rules`, data);
    return response.data;
  },

  async agreeToRules(householdId: string): Promise<void> {
    await api.post(`/households/${householdId}/rules/agree`);
  },

  // Bill Reminders
  async getBillReminders(householdId: string): Promise<BillReminder[]> {
    const response = await api.get(`/households/${householdId}/bill-reminders`);
    return response.data;
  },

  async createBillReminder(
    householdId: string,
    data: {
      name: string;
      category: ExpenseCategory;
      amount?: number;
      isRecurring?: boolean;
      dueDay?: number;
      reminderDaysBefore?: number;
    }
  ): Promise<BillReminder> {
    const response = await api.post(
      `/households/${householdId}/bill-reminders`,
      data
    );
    return response.data;
  },

  // Tasks
  async getTasks(householdId: string): Promise<Task[]> {
    const response = await api.get(`/households/${householdId}/tasks`);
    return response.data;
  },

  async getMyTasks(): Promise<Task[]> {
    const response = await api.get("/households/my-tasks");
    return response.data;
  },

  async createTask(
    householdId: string,
    data: {
      title: string;
      description?: string;
      category: TaskCategory;
      assignedTo?: string;
      assignmentType?: AssignmentType;
      dueDate?: string;
      dueTime?: string;
      isRecurring?: boolean;
      recurrencePattern?: RecurrencePattern;
      recurrenceDay?: number;
      priority?: TaskPriority;
    }
  ): Promise<Task> {
    const response = await api.post(`/households/${householdId}/tasks`, data);
    return response.data;
  },

  async completeTask(taskId: string): Promise<Task> {
    const response = await api.post(`/households/tasks/${taskId}/complete`);
    return response.data;
  },

  // Issues
  async getIssues(householdId: string): Promise<Issue[]> {
    const response = await api.get(`/households/${householdId}/issues`);
    return response.data;
  },

  async createIssue(
    householdId: string,
    data: {
      title: string;
      description: string;
      category: IssueCategory;
      severity?: IssueSeverity;
      reportedAgainst?: string;
      isAnonymous?: boolean;
    }
  ): Promise<Issue> {
    const response = await api.post(`/households/${householdId}/issues`, data);
    return response.data;
  },

  async addComment(
    issueId: string,
    content: string,
    isResolutionProposal = false
  ): Promise<Issue> {
    const response = await api.post(`/households/issues/${issueId}/comments`, {
      content,
      isResolutionProposal,
    });
    return response.data;
  },

  async resolveIssue(issueId: string, resolution: string): Promise<Issue> {
    const response = await api.post(`/households/issues/${issueId}/resolve`, {
      resolution,
    });
    return response.data;
  },

  async escalateIssue(issueId: string, escalateTo: string): Promise<void> {
    await api.post(`/households/issues/${issueId}/escalate`, { escalateTo });
  },
};

export default householdService;
