export const DEMO_USERS = [
  { id: 7, username: 'employee1', name: 'Sample Employee 01', role: 'EMPLOYEE', serviceGroup: 'A', email: 'employee1@test.com' },
  { id: 8, username: 'ddo1', name: 'Sample DDO 01', role: 'DDO', serviceGroup: 'B', email: 'ddo1@test.com' },
  { id: 9, username: 'hoo1', name: 'Sample HOO 01', role: 'HOO', serviceGroup: 'A', email: 'hoo1@test.com' },
  { id: 1, username: 'admin', name: 'Sample Administrator', role: 'ADMIN', email: 'admin@loanmodule.com' }
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
