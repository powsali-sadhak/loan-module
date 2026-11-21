export const DEMO_USERS = [
  { id: 7, username: 'employee1', name: 'Test Employee', role: 'EMPLOYEE', email: 'employee1@test.com' },
  { id: 8, username: 'ddo1', name: 'Test DDO', role: 'DDO', email: 'ddo1@test.com' },
  { id: 9, username: 'hoo1', name: 'Test HOO', role: 'HOO', email: 'hoo1@test.com' },
  { id: 1, username: 'admin', name: 'System Administrator', role: 'ADMIN', email: 'admin@loanmodule.com' }
]
export const money = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(value || 0))
export const dateTime = value => value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—'
export const titleCase = value => String(value || '').toLowerCase().replaceAll('_', ' ').replace(/\\b\\w/g, c => c.toUpperCase())
export const badgeClass = value => {
  const key = String(value || '').toLowerCase()
  if (['approved', 'resolved', 'closed', 'active'].includes(key)) return 'success'
  if (['rejected', 'urgent', 'revision_requested'].includes(key)) return 'danger'
  if (['pending_approval', 'in_progress', 'assigned', 'high'].includes(key)) return 'warning'
  return 'neutral'
}