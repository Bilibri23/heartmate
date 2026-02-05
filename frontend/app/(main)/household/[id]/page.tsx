"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/context/auth-context";
import householdService, {
  Household,
  Expense,
  BalanceSummary,
  HouseRules,
  Task,
  Issue,
  BillReminder,
  ExpenseCategory,
  SplitType,
  TaskCategory,
  TaskPriority,
  IssueCategory,
  IssueSeverity,
  GuestPolicy,
  CleaningSchedule,
} from "@/services/household";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Home,
  Users,
  DollarSign,
  ClipboardList,
  AlertCircle,
  Plus,
  ArrowLeft,
  Loader2,
  Check,
  X,
  Send,
  Receipt,
  Calendar,
  Clock,
  Scroll,
  UserPlus,
  Settings,
  ChevronDown,
  ChevronUp,
  MessageSquare,
  Flag,
  CheckCircle,
  CircleAlert,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";

export default function HouseholdDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const householdId = params.id as string;

  const [household, setHousehold] = useState<Household | null>(null);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [balance, setBalance] = useState<BalanceSummary | null>(null);
  const [rules, setRules] = useState<HouseRules | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [billReminders, setBillReminders] = useState<BillReminder[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("overview");

  // Modal states
  const [showExpenseModal, setShowExpenseModal] = useState(false);
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [showSettlementModal, setShowSettlementModal] = useState(false);
  const [showRulesModal, setShowRulesModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      const [householdData, expenseData, balanceData, rulesData, taskData, issueData, reminderData] =
        await Promise.all([
          householdService.getHousehold(householdId),
          householdService.getExpenses(householdId),
          householdService.getBalanceSummary(householdId),
          householdService.getHouseRules(householdId),
          householdService.getTasks(householdId),
          householdService.getIssues(householdId),
          householdService.getBillReminders(householdId),
        ]);

      setHousehold(householdData);
      setExpenses(expenseData.content);
      setBalance(balanceData);
      setRules(rulesData);
      setTasks(taskData);
      setIssues(issueData);
      setBillReminders(reminderData);
    } catch (error) {
      console.error("Error fetching household data:", error);
      toast.error("Failed to load household data");
    } finally {
      setLoading(false);
    }
  }, [householdId]);

  useEffect(() => {
    if (user && householdId) {
      fetchData();
    }
  }, [user, householdId, fetchData]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("fr-CM", {
      style: "currency",
      currency: "XAF",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  // Check if current user is admin of this household
  const isAdmin = household?.members.some(
    (m) => m.userId === user?.id && m.role === "ADMIN"
  );

  // Check if household can be deleted (not linked to a lease)
  const canDelete = isAdmin && !household?.leaseId;

  const handleDeleteHousehold = async () => {
    if (!canDelete) return;
    
    setIsDeleting(true);
    try {
      await householdService.deleteHousehold(householdId);
      toast.success("Household deleted successfully");
      router.push("/household");
    } catch (error: any) {
      console.error("Error deleting household:", error);
      toast.error(error.response?.data?.message || "Failed to delete household");
    } finally {
      setIsDeleting(false);
      setShowDeleteModal(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (!household) {
    return (
      <div className="container mx-auto px-4 py-6">
        <Card>
          <CardContent className="py-12 text-center">
            <AlertCircle className="h-12 w-12 mx-auto text-slate-300 mb-4" />
            <h3 className="text-lg font-medium text-slate-900">
              Household Not Found
            </h3>
            <Button className="mt-4" onClick={() => router.push("/household")}>
              Back to Households
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-6 max-w-4xl">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push("/household")}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold text-slate-900">{household.name}</h1>
          {household.listingTitle && (
            <p className="text-slate-500">{household.listingTitle}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <div className="flex -space-x-2">
            {household.members.slice(0, 3).map((member) => (
              <div
                key={member.id}
                className="h-8 w-8 rounded-full bg-blue-100 border-2 border-white flex items-center justify-center"
                title={`${member.firstName} ${member.lastName}`}
              >
                {member.photoUrl ? (
                  <img
                    src={member.photoUrl}
                    alt={member.firstName}
                    className="h-full w-full rounded-full object-cover"
                  />
                ) : (
                  <span className="text-xs font-medium text-blue-600">
                    {member.firstName[0]}
                  </span>
                )}
              </div>
            ))}
            {household.members.length > 3 && (
              <div className="h-8 w-8 rounded-full bg-slate-100 border-2 border-white flex items-center justify-center">
                <span className="text-xs font-medium text-slate-600">
                  +{household.members.length - 3}
                </span>
              </div>
            )}
          </div>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="text-white-500 hover:text-red-600 hover:bg-red-50"
              onClick={() => setShowDeleteModal(true)}
              title="Delete household"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle className="text-white-600 flex items-center gap-2">
                <Trash2 className="h-5 w-5" />
                Delete Household
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-slate-600 mb-4">
                Are you sure you want to delete &quot;{household.name}&quot;? This action cannot be undone.
              </p>
              <p className="text-sm text-slate-500 mb-6">
                All expenses, tasks, issues, and rules associated with this household will be permanently deleted.
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setShowDeleteModal(false)}
                  disabled={isDeleting}
                >
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  className="flex-1"
                  onClick={handleDeleteHousehold}
                  disabled={isDeleting}
                >
                  {isDeleting ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    "Delete"
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid grid-cols-5 mb-6">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="expenses">Expenses</TabsTrigger>
          <TabsTrigger value="tasks">Tasks</TabsTrigger>
          <TabsTrigger value="rules">Rules</TabsTrigger>
          <TabsTrigger value="issues">Issues</TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview">
          <div className="space-y-6">
            {/* Balance Summary */}
            {balance && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <DollarSign className="h-5 w-5" />
                    Balance Summary
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-3 gap-4 mb-6">
                    <div className="text-center p-4 bg-green-50 rounded-lg">
                      <p className="text-sm text-green-600">You Paid</p>
                      <p className="text-xl font-bold text-green-700">
                        {formatCurrency(balance.totalPaidByMe)}
                      </p>
                    </div>
                    <div className="text-center p-4 bg-red-50 rounded-lg">
                      <p className="text-sm text-red-600">You Owe</p>
                      <p className="text-xl font-bold text-red-700">
                        {formatCurrency(balance.totalOwedByMe)}
                      </p>
                    </div>
                    <div className="text-center p-4 bg-blue-50 rounded-lg">
                      <p className="text-sm text-blue-600">Owed to You</p>
                      <p className="text-xl font-bold text-blue-700">
                        {formatCurrency(balance.totalOwedToMe)}
                      </p>
                    </div>
                  </div>

                  {balance.memberBalances.length > 0 && (
                    <div>
                      <h4 className="text-sm font-medium text-slate-700 mb-3">
                        Member Balances
                      </h4>
                      <div className="space-y-2">
                        {balance.memberBalances.map((mb) => (
                          <div
                            key={mb.userId}
                            className="flex items-center justify-between p-3 bg-slate-50 rounded-lg"
                          >
                            <div className="flex items-center gap-3">
                              <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center">
                                {mb.userPhoto ? (
                                  <img
                                    src={mb.userPhoto}
                                    alt={mb.userName}
                                    className="h-full w-full rounded-full object-cover"
                                  />
                                ) : (
                                  <span className="text-sm font-medium text-blue-600">
                                    {mb.userName[0]}
                                  </span>
                                )}
                              </div>
                              <span className="font-medium">{mb.userName}</span>
                            </div>
                            <span
                              className={`font-semibold ${
                                mb.amountOwed > 0
                                  ? "text-green-600"
                                  : mb.amountOwed < 0
                                  ? "text-red-600"
                                  : "text-slate-500"
                              }`}
                            >
                              {mb.amountOwed > 0
                                ? `owes you ${formatCurrency(mb.amountOwed)}`
                                : mb.amountOwed < 0
                                ? `you owe ${formatCurrency(Math.abs(mb.amountOwed))}`
                                : "settled"}
                            </span>
                          </div>
                        ))}
                      </div>
                      <Button
                        className="w-full mt-4"
                        variant="outline"
                        onClick={() => setShowSettlementModal(true)}
                      >
                        <Send className="h-4 w-4 mr-2" />
                        Record Settlement
                      </Button>
                    </div>
                  )}
                </CardContent>
              </Card>
            )}

            {/* Members */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <Users className="h-5 w-5" />
                    Members ({household.members.length})
                  </span>
                  <Button size="sm" variant="outline">
                    <UserPlus className="h-4 w-4 mr-2" />
                    Invite
                  </Button>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {household.members.map((member) => (
                    <div
                      key={member.id}
                      className="flex items-center justify-between p-3 bg-slate-50 rounded-lg"
                    >
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                          {member.photoUrl ? (
                            <img
                              src={member.photoUrl}
                              alt={member.firstName}
                              className="h-full w-full rounded-full object-cover"
                            />
                          ) : (
                            <span className="text-sm font-medium text-blue-600">
                              {member.firstName[0]}
                            </span>
                          )}
                        </div>
                        <div>
                          <p className="font-medium">
                            {member.firstName} {member.lastName}
                            {member.role === "ADMIN" && (
                              <span className="ml-2 text-xs bg-blue-100 text-blue-600 px-2 py-0.5 rounded">
                                Admin
                              </span>
                            )}
                          </p>
                          <p className="text-sm text-slate-500">{member.email}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-medium">
                          {member.rentSharePercentage}%
                        </p>
                        <p className="text-xs text-slate-500">rent share</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Upcoming Reminders */}
            {billReminders.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Calendar className="h-5 w-5" />
                    Bill Reminders
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {billReminders.slice(0, 3).map((reminder) => (
                      <div
                        key={reminder.id}
                        className="flex items-center justify-between p-3 bg-slate-50 rounded-lg"
                      >
                        <div>
                          <p className="font-medium">{reminder.name}</p>
                          <p className="text-sm text-slate-500">
                            Due on the {reminder.dueDay}th
                          </p>
                        </div>
                        <div className="text-right">
                          {reminder.amount && (
                            <p className="font-medium">
                              {formatCurrency(reminder.amount)}
                            </p>
                          )}
                          <p
                            className={`text-sm ${
                              reminder.isPastDue
                                ? "text-red-600"
                                : reminder.daysUntilDue <= 3
                                ? "text-orange-600"
                                : "text-slate-500"
                            }`}
                          >
                            {reminder.isPastDue
                              ? "Past due"
                              : `${reminder.daysUntilDue} days left`}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}
          </div>
        </TabsContent>

        {/* Expenses Tab */}
        <TabsContent value="expenses">
          <ExpensesTab
            expenses={expenses}
            household={household}
            formatCurrency={formatCurrency}
            formatDate={formatDate}
            onAddExpense={() => setShowExpenseModal(true)}
            onRefresh={fetchData}
          />
        </TabsContent>

        {/* Tasks Tab */}
        <TabsContent value="tasks">
          <TasksTab
            tasks={tasks}
            household={household}
            formatDate={formatDate}
            onAddTask={() => setShowTaskModal(true)}
            onRefresh={fetchData}
          />
        </TabsContent>

        {/* Rules Tab */}
        <TabsContent value="rules">
          <RulesTab
            rules={rules}
            household={household}
            onEditRules={() => setShowRulesModal(true)}
            onAgree={async () => {
              try {
                await householdService.agreeToRules(householdId);
                toast.success("You've agreed to the house rules!");
                fetchData();
              } catch (error) {
                toast.error("Failed to agree to rules");
              }
            }}
          />
        </TabsContent>

        {/* Issues Tab */}
        <TabsContent value="issues">
          <IssuesTab
            issues={issues}
            household={household}
            formatDate={formatDate}
            onAddIssue={() => setShowIssueModal(true)}
            onRefresh={fetchData}
          />
        </TabsContent>
      </Tabs>

      {/* Add Expense Modal */}
      {showExpenseModal && (
        <AddExpenseModal
          householdId={householdId}
          members={household.members}
          onClose={() => setShowExpenseModal(false)}
          onSuccess={() => {
            setShowExpenseModal(false);
            fetchData();
          }}
        />
      )}

      {/* Add Task Modal */}
      {showTaskModal && (
        <AddTaskModal
          householdId={householdId}
          members={household.members}
          onClose={() => setShowTaskModal(false)}
          onSuccess={() => {
            setShowTaskModal(false);
            fetchData();
          }}
        />
      )}

      {/* Add Issue Modal */}
      {showIssueModal && (
        <AddIssueModal
          householdId={householdId}
          members={household.members}
          onClose={() => setShowIssueModal(false)}
          onSuccess={() => {
            setShowIssueModal(false);
            fetchData();
          }}
        />
      )}

      {/* Settlement Modal */}
      {showSettlementModal && balance && (
        <SettlementModal
          householdId={householdId}
          members={household.members}
          balance={balance}
          onClose={() => setShowSettlementModal(false)}
          onSuccess={() => {
            setShowSettlementModal(false);
            fetchData();
          }}
        />
      )}

      {/* Edit Rules Modal */}
      {showRulesModal && rules && (
        <EditRulesModal
          householdId={householdId}
          rules={rules}
          onClose={() => setShowRulesModal(false)}
          onSuccess={() => {
            setShowRulesModal(false);
            fetchData();
          }}
        />
      )}
    </div>
  );
}

// Expenses Tab Component
function ExpensesTab({
  expenses,
  household,
  formatCurrency,
  formatDate,
  onAddExpense,
  onRefresh,
}: {
  expenses: Expense[];
  household: Household;
  formatCurrency: (amount: number) => string;
  formatDate: (date: string) => string;
  onAddExpense: () => void;
  onRefresh: () => void;
}) {
  const getCategoryIcon = (category: ExpenseCategory) => {
    const icons: Record<ExpenseCategory, string> = {
      RENT: "🏠",
      UTILITIES: "💡",
      GROCERIES: "🛒",
      SUPPLIES: "📦",
      INTERNET: "📶",
      CLEANING: "🧹",
      WATER: "💧",
      ELECTRICITY: "⚡",
      GAS: "🔥",
      OTHER: "📝",
    };
    return icons[category] || "📝";
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">Recent Expenses</h3>
        <Button onClick={onAddExpense}>
          <Plus className="h-4 w-4 mr-2" />
          Add Expense
        </Button>
      </div>

      {expenses.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Receipt className="h-12 w-12 mx-auto text-slate-300 mb-4" />
            <h3 className="text-lg font-medium text-slate-900">No Expenses Yet</h3>
            <p className="text-slate-500 mt-2">
              Add your first expense to start tracking
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {expenses.map((expense) => (
            <Card key={expense.id}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <div className="h-10 w-10 rounded-lg bg-slate-100 flex items-center justify-center text-xl">
                      {getCategoryIcon(expense.category)}
                    </div>
                    <div>
                      <p className="font-medium">{expense.description}</p>
                      <p className="text-sm text-slate-500">
                        Paid by {expense.paidByName} • {formatDate(expense.expenseDate)}
                      </p>
                    </div>
                  </div>
                  <p className="font-bold text-lg">{formatCurrency(expense.amount)}</p>
                </div>

                <div className="mt-3 pt-3 border-t">
                  <p className="text-xs text-slate-500 mb-2">
                    Split {expense.splitType.toLowerCase()}
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {expense.splits.map((split) => (
                      <div
                        key={split.id}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
                          split.isPaid
                            ? "bg-green-50 text-green-700"
                            : "bg-red-50 text-red-700"
                        }`}
                      >
                        {split.isPaid ? (
                          <Check className="h-3 w-3" />
                        ) : (
                          <Clock className="h-3 w-3" />
                        )}
                        {split.userName}: {formatCurrency(split.amount)}
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// Tasks Tab Component
function TasksTab({
  tasks,
  household,
  formatDate,
  onAddTask,
  onRefresh,
}: {
  tasks: Task[];
  household: Household;
  formatDate: (date: string) => string;
  onAddTask: () => void;
  onRefresh: () => void;
}) {
  const handleCompleteTask = async (taskId: string) => {
    try {
      await householdService.completeTask(taskId);
      toast.success("Task completed!");
      onRefresh();
    } catch (error) {
      toast.error("Failed to complete task");
    }
  };

  const getCategoryIcon = (category: TaskCategory) => {
    const icons: Record<TaskCategory, string> = {
      CLEANING: "🧹",
      SHOPPING: "🛒",
      MAINTENANCE: "🔧",
      COOKING: "👨‍🍳",
      LAUNDRY: "👕",
      TRASH: "🗑️",
      OTHER: "📋",
    };
    return icons[category] || "📋";
  };

  const getPriorityColor = (priority: TaskPriority) => {
    const colors: Record<TaskPriority, string> = {
      LOW: "text-slate-500 bg-slate-100",
      NORMAL: "text-blue-600 bg-blue-100",
      HIGH: "text-orange-600 bg-orange-100",
      URGENT: "text-red-600 bg-red-100",
    };
    return colors[priority] || colors.NORMAL;
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">Household Tasks</h3>
        <Button onClick={onAddTask}>
          <Plus className="h-4 w-4 mr-2" />
          Add Task
        </Button>
      </div>

      {tasks.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <ClipboardList className="h-12 w-12 mx-auto text-slate-300 mb-4" />
            <h3 className="text-lg font-medium text-slate-900">No Tasks Yet</h3>
            <p className="text-slate-500 mt-2">
              Create tasks to coordinate with your roommates
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <Card key={task.id} className={task.isOverdue ? "border-red-200" : ""}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3">
                    <div className="h-10 w-10 rounded-lg bg-slate-100 flex items-center justify-center text-xl">
                      {getCategoryIcon(task.category)}
                    </div>
                    <div>
                      <p className="font-medium">{task.title}</p>
                      {task.description && (
                        <p className="text-sm text-slate-500">{task.description}</p>
                      )}
                      <div className="flex items-center gap-2 mt-1">
                        {task.dueDate && (
                          <span
                            className={`text-xs ${
                              task.isOverdue ? "text-red-600" : "text-slate-500"
                            }`}
                          >
                            Due: {formatDate(task.dueDate)}
                          </span>
                        )}
                        {task.assignedToName && (
                          <span className="text-xs text-slate-500">
                            • Assigned to: {task.assignedToName}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs px-2 py-1 rounded ${getPriorityColor(
                        task.priority
                      )}`}
                    >
                      {task.priority}
                    </span>
                    {task.status === "PENDING" && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleCompleteTask(task.id)}
                      >
                        <Check className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// Rules Tab Component
function RulesTab({
  rules,
  household,
  onEditRules,
  onAgree,
}: {
  rules: HouseRules | null;
  household: Household;
  onEditRules: () => void;
  onAgree: () => void;
}) {
  if (!rules) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <Scroll className="h-12 w-12 mx-auto text-slate-300 mb-4" />
          <h3 className="text-lg font-medium text-slate-900">No Rules Set</h3>
          <p className="text-slate-500 mt-2">
            Set up house rules to align expectations
          </p>
          <Button className="mt-4" onClick={onEditRules}>
            <Settings className="h-4 w-4 mr-2" />
            Set Up Rules
          </Button>
        </CardContent>
      </Card>
    );
  }

  const guestPolicyLabels: Record<GuestPolicy, string> = {
    ALWAYS_OK: "Guests welcome anytime",
    NOTIFY: "Notify roommates when having guests",
    ASK_FIRST: "Ask permission first",
    NO_OVERNIGHT: "No overnight guests",
    NO_GUESTS: "No guests allowed",
  };

  const cleaningLabels: Record<CleaningSchedule, string> = {
    ROTATING: "Members take turns",
    ASSIGNED: "Fixed assignments",
    FLEXIBLE: "Clean as needed",
    SHARED: "Clean together",
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">House Rules</h3>
        <Button variant="outline" onClick={onEditRules}>
          <Settings className="h-4 w-4 mr-2" />
          Edit Rules
        </Button>
      </div>

      {/* Agreement Status */}
      {!rules.allMembersAgreed && (
        <Card className="border-orange-200 bg-orange-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-5 w-5 text-orange-600" />
                <span className="font-medium text-orange-800">
                  Not all members have agreed to the rules
                </span>
              </div>
              {!rules.currentUserAgreed && (
                <Button size="sm" onClick={onAgree}>
                  <Check className="h-4 w-4 mr-2" />
                  Agree
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-4 space-y-4">
          {/* Quiet Hours */}
          <div className="flex items-center justify-between py-2 border-b">
            <span className="text-slate-600">Quiet Hours</span>
            <span className="font-medium">
              {rules.quietHoursEnabled
                ? `${rules.quietHoursStart} - ${rules.quietHoursEnd}`
                : "Not set"}
            </span>
          </div>

          {/* Guest Policy */}
          <div className="flex items-center justify-between py-2 border-b">
            <span className="text-slate-600">Guest Policy</span>
            <span className="font-medium">
              {guestPolicyLabels[rules.guestPolicy]}
            </span>
          </div>

          {/* Cleaning */}
          <div className="flex items-center justify-between py-2 border-b">
            <span className="text-slate-600">Cleaning Schedule</span>
            <span className="font-medium">
              {cleaningLabels[rules.cleaningSchedule]}
            </span>
          </div>

          {/* Smoking */}
          <div className="flex items-center justify-between py-2 border-b">
            <span className="text-slate-600">Smoking</span>
            <span className="font-medium">
              {rules.smokingAllowed
                ? rules.smokingArea
                  ? `Allowed in ${rules.smokingArea}`
                  : "Allowed"
                : "Not allowed"}
            </span>
          </div>

          {/* Pets */}
          <div className="flex items-center justify-between py-2 border-b">
            <span className="text-slate-600">Pets</span>
            <span className="font-medium">
              {rules.petsAllowed ? "Allowed" : "Not allowed"}
            </span>
          </div>

          {/* Custom Rules */}
          {rules.customRules && rules.customRules.length > 0 && (
            <div className="py-2">
              <span className="text-slate-600 block mb-2">Custom Rules</span>
              <ul className="list-disc list-inside space-y-1">
                {rules.customRules.map((rule, index) => (
                  <li key={index} className="text-sm text-slate-700">
                    {rule}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Agreement List */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Member Agreements</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {rules.agreements.map((agreement) => (
              <div
                key={agreement.userId}
                className="flex items-center justify-between p-2"
              >
                <div className="flex items-center gap-2">
                  {agreement.hasAgreed ? (
                    <CheckCircle className="h-5 w-5 text-green-600" />
                  ) : (
                    <Clock className="h-5 w-5 text-slate-400" />
                  )}
                  <span>{agreement.userName}</span>
                </div>
                <span className="text-sm text-slate-500">
                  {agreement.hasAgreed
                    ? `Agreed on ${new Date(agreement.agreedAt!).toLocaleDateString()}`
                    : "Pending"}
                </span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Issues Tab Component
function IssuesTab({
  issues,
  household,
  formatDate,
  onAddIssue,
  onRefresh,
}: {
  issues: Issue[];
  household: Household;
  formatDate: (date: string) => string;
  onAddIssue: () => void;
  onRefresh: () => void;
}) {
  const getSeverityColor = (severity: IssueSeverity) => {
    const colors: Record<IssueSeverity, string> = {
      LOW: "text-slate-500 bg-slate-100",
      MEDIUM: "text-yellow-600 bg-yellow-100",
      HIGH: "text-orange-600 bg-orange-100",
      CRITICAL: "text-red-600 bg-red-100",
    };
    return colors[severity] || colors.MEDIUM;
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      OPEN: "text-blue-600 bg-blue-100",
      ACKNOWLEDGED: "text-purple-600 bg-purple-100",
      IN_DISCUSSION: "text-yellow-600 bg-yellow-100",
      RESOLVED: "text-green-600 bg-green-100",
      ESCALATED: "text-red-600 bg-red-100",
      CLOSED: "text-slate-500 bg-slate-100",
    };
    return colors[status] || colors.OPEN;
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">Household Issues</h3>
        <Button onClick={onAddIssue}>
          <Plus className="h-4 w-4 mr-2" />
          Report Issue
        </Button>
      </div>

      {issues.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <CheckCircle className="h-12 w-12 mx-auto text-green-300 mb-4" />
            <h3 className="text-lg font-medium text-slate-900">No Open Issues</h3>
            <p className="text-slate-500 mt-2">
              Everything is running smoothly!
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {issues.map((issue) => (
            <Card key={issue.id}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`text-xs px-2 py-1 rounded ${getSeverityColor(issue.severity)}`}>
                        {issue.severity}
                      </span>
                      <span className={`text-xs px-2 py-1 rounded ${getStatusColor(issue.status)}`}>
                        {issue.status.replace("_", " ")}
                      </span>
                    </div>
                    <p className="font-medium">{issue.title}</p>
                    <p className="text-sm text-slate-500 mt-1">{issue.description}</p>
                    <p className="text-xs text-slate-400 mt-2">
                      Reported by {issue.isAnonymous ? "Anonymous" : issue.reportedByName} •{" "}
                      {formatDate(issue.createdAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 text-slate-400">
                    <MessageSquare className="h-4 w-4" />
                    <span className="text-sm">{issue.commentCount}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// Add Expense Modal
function AddExpenseModal({
  householdId,
  members,
  onClose,
  onSuccess,
}: {
  householdId: string;
  members: Household["members"];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState<ExpenseCategory>("OTHER");
  const [description, setDescription] = useState("");
  const [splitType, setSplitType] = useState<SplitType>("EQUAL");
  const [submitting, setSubmitting] = useState(false);

  const categories: ExpenseCategory[] = [
    "RENT", "UTILITIES", "GROCERIES", "SUPPLIES", "INTERNET", 
    "CLEANING", "WATER", "ELECTRICITY", "GAS", "OTHER"
  ];

  const handleSubmit = async () => {
    if (!amount || !description) {
      toast.error("Please fill in all required fields");
      return;
    }

    setSubmitting(true);
    try {
      await householdService.createExpense(householdId, {
        amount: parseInt(amount),
        category,
        description,
        splitType,
      });
      toast.success("Expense added successfully!");
      onSuccess();
    } catch (error) {
      toast.error("Failed to add expense");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <CardTitle>Add Expense</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Amount (XAF) *
            </label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="e.g., 5000"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Category *
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value as ExpenseCategory)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat.charAt(0) + cat.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Description *
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g., Monthly electricity bill"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Split Type
            </label>
            <select
              value={splitType}
              onChange={(e) => setSplitType(e.target.value as SplitType)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="EQUAL">Split equally</option>
              <option value="PERCENTAGE">By rent share percentage</option>
            </select>
          </div>

          <div className="flex gap-2 pt-4">
            <Button variant="outline" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button className="flex-1" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : "Add Expense"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Add Task Modal
function AddTaskModal({
  householdId,
  members,
  onClose,
  onSuccess,
}: {
  householdId: string;
  members: Household["members"];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<TaskCategory>("OTHER");
  const [assignedTo, setAssignedTo] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [priority, setPriority] = useState<TaskPriority>("NORMAL");
  const [submitting, setSubmitting] = useState(false);

  const categories: TaskCategory[] = [
    "CLEANING", "SHOPPING", "MAINTENANCE", "COOKING", "LAUNDRY", "TRASH", "OTHER"
  ];

  const handleSubmit = async () => {
    if (!title) {
      toast.error("Please enter a task title");
      return;
    }

    setSubmitting(true);
    try {
      await householdService.createTask(householdId, {
        title,
        description: description || undefined,
        category,
        assignedTo: assignedTo || undefined,
        dueDate: dueDate || undefined,
        priority,
      });
      toast.success("Task created successfully!");
      onSuccess();
    } catch (error) {
      toast.error("Failed to create task");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <CardTitle>Add Task</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Task Title *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g., Clean the kitchen"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Category
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value as TaskCategory)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat.charAt(0) + cat.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Assign To
            </label>
            <select
              value={assignedTo}
              onChange={(e) => setAssignedTo(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Anyone (voluntary)</option>
              {members.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.firstName} {member.lastName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Due Date
            </label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Priority
            </label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="LOW">Low</option>
              <option value="NORMAL">Normal</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Additional details..."
              rows={3}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button variant="outline" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button className="flex-1" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : "Create Task"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Add Issue Modal
function AddIssueModal({
  householdId,
  members,
  onClose,
  onSuccess,
}: {
  householdId: string;
  members: Household["members"];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<IssueCategory>("OTHER");
  const [severity, setSeverity] = useState<IssueSeverity>("MEDIUM");
  const [reportedAgainst, setReportedAgainst] = useState("");
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const categories: IssueCategory[] = [
    "NOISE", "CLEANLINESS", "BILLS", "GUESTS", "BEHAVIOR", "PROPERTY", "RULES_VIOLATION", "OTHER"
  ];

  const handleSubmit = async () => {
    if (!title || !description) {
      toast.error("Please fill in all required fields");
      return;
    }

    setSubmitting(true);
    try {
      await householdService.createIssue(householdId, {
        title,
        description,
        category,
        severity,
        reportedAgainst: reportedAgainst || undefined,
        isAnonymous,
      });
      toast.success("Issue reported successfully!");
      onSuccess();
    } catch (error) {
      toast.error("Failed to report issue");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <CardTitle>Report Issue</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Issue Title *
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g., Noise after quiet hours"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Category
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value as IssueCategory)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat.replace("_", " ").charAt(0) + cat.replace("_", " ").slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Severity
            </label>
            <select
              value={severity}
              onChange={(e) => setSeverity(e.target.value as IssueSeverity)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Regarding (optional)
            </label>
            <select
              value={reportedAgainst}
              onChange={(e) => setReportedAgainst(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">General issue</option>
              {members.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.firstName} {member.lastName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Description *
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the issue in detail..."
              rows={4}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="anonymous"
              checked={isAnonymous}
              onChange={(e) => setIsAnonymous(e.target.checked)}
              className="h-4 w-4"
            />
            <label htmlFor="anonymous" className="text-sm text-slate-600">
              Report anonymously
            </label>
          </div>

          <div className="flex gap-2 pt-4">
            <Button variant="outline" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button className="flex-1" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : "Report Issue"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Settlement Modal
function SettlementModal({
  householdId,
  members,
  balance,
  onClose,
  onSuccess,
}: {
  householdId: string;
  members: Household["members"];
  balance: BalanceSummary;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [toUserId, setToUserId] = useState("");
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!toUserId || !amount) {
      toast.error("Please select a recipient and enter an amount");
      return;
    }

    setSubmitting(true);
    try {
      await householdService.recordSettlement(householdId, {
        toUserId,
        amount: parseInt(amount),
        note: note || undefined,
        settlementMethod: "CASH",
      });
      toast.success("Settlement recorded!");
      onSuccess();
    } catch (error) {
      toast.error("Failed to record settlement");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Record Settlement</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Paying To *
            </label>
            <select
              value={toUserId}
              onChange={(e) => setToUserId(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Select member</option>
              {members
                .filter((m) => m.userId !== balance.userId)
                .map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.firstName} {member.lastName}
                  </option>
                ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Amount (XAF) *
            </label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="e.g., 5000"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Note (optional)
            </label>
            <input
              type="text"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g., For electricity bill"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button variant="outline" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button className="flex-1" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : "Record"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// Edit Rules Modal
function EditRulesModal({
  householdId,
  rules,
  onClose,
  onSuccess,
}: {
  householdId: string;
  rules: HouseRules;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [quietHoursEnabled, setQuietHoursEnabled] = useState(rules.quietHoursEnabled);
  const [quietHoursStart, setQuietHoursStart] = useState(rules.quietHoursStart || "22:00");
  const [quietHoursEnd, setQuietHoursEnd] = useState(rules.quietHoursEnd || "08:00");
  const [guestPolicy, setGuestPolicy] = useState<GuestPolicy>(rules.guestPolicy);
  const [cleaningSchedule, setCleaningSchedule] = useState<CleaningSchedule>(rules.cleaningSchedule);
  const [smokingAllowed, setSmokingAllowed] = useState(rules.smokingAllowed);
  const [petsAllowed, setPetsAllowed] = useState(rules.petsAllowed);
  const [customRules, setCustomRules] = useState(rules.customRules?.join("\n") || "");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      await householdService.updateHouseRules(householdId, {
        quietHoursEnabled,
        quietHoursStart: quietHoursEnabled ? quietHoursStart : undefined,
        quietHoursEnd: quietHoursEnabled ? quietHoursEnd : undefined,
        guestPolicy,
        cleaningSchedule,
        smokingAllowed,
        petsAllowed,
        customRules: customRules.split("\n").filter(r => r.trim()),
      });
      toast.success("House rules updated! Members will need to re-agree.");
      onSuccess();
    } catch (error) {
      toast.error("Failed to update rules");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <Card className="w-full max-w-md max-h-[90vh] overflow-y-auto">
        <CardHeader>
          <CardTitle>Edit House Rules</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Quiet Hours */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-sm font-medium text-slate-700">Quiet Hours</label>
              <input
                type="checkbox"
                checked={quietHoursEnabled}
                onChange={(e) => setQuietHoursEnabled(e.target.checked)}
                className="h-4 w-4"
              />
            </div>
            {quietHoursEnabled && (
              <div className="flex gap-2">
                <input
                  type="time"
                  value={quietHoursStart}
                  onChange={(e) => setQuietHoursStart(e.target.value)}
                  className="flex-1 px-3 py-2 border border-slate-300 rounded-lg"
                />
                <span className="self-center">to</span>
                <input
                  type="time"
                  value={quietHoursEnd}
                  onChange={(e) => setQuietHoursEnd(e.target.value)}
                  className="flex-1 px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>
            )}
          </div>

          {/* Guest Policy */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Guest Policy
            </label>
            <select
              value={guestPolicy}
              onChange={(e) => setGuestPolicy(e.target.value as GuestPolicy)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
            >
              <option value="ALWAYS_OK">Guests welcome anytime</option>
              <option value="NOTIFY">Notify roommates</option>
              <option value="ASK_FIRST">Ask permission first</option>
              <option value="NO_OVERNIGHT">No overnight guests</option>
              <option value="NO_GUESTS">No guests allowed</option>
            </select>
          </div>

          {/* Cleaning Schedule */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Cleaning Schedule
            </label>
            <select
              value={cleaningSchedule}
              onChange={(e) => setCleaningSchedule(e.target.value as CleaningSchedule)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
            >
              <option value="ROTATING">Rotating turns</option>
              <option value="ASSIGNED">Fixed assignments</option>
              <option value="FLEXIBLE">Clean as needed</option>
              <option value="SHARED">Clean together</option>
            </select>
          </div>

          {/* Smoking */}
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium text-slate-700">Smoking Allowed</label>
            <input
              type="checkbox"
              checked={smokingAllowed}
              onChange={(e) => setSmokingAllowed(e.target.checked)}
              className="h-4 w-4"
            />
          </div>

          {/* Pets */}
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium text-slate-700">Pets Allowed</label>
            <input
              type="checkbox"
              checked={petsAllowed}
              onChange={(e) => setPetsAllowed(e.target.checked)}
              className="h-4 w-4"
            />
          </div>

          {/* Custom Rules */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Custom Rules (one per line)
            </label>
            <textarea
              value={customRules}
              onChange={(e) => setCustomRules(e.target.value)}
              placeholder="Add custom rules..."
              rows={4}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg"
            />
          </div>

          <div className="flex gap-2 pt-4">
            <Button variant="outline" className="flex-1" onClick={onClose}>
              Cancel
            </Button>
            <Button className="flex-1" onClick={handleSubmit} disabled={submitting}>
              {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : "Save Rules"}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
