import React, { useEffect, useMemo, useState } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  Activity, ArrowDownLeft, ArrowUpRight, Bell, BriefcaseBusiness, ChevronDown, CircleHelp,
  ClipboardCheck, CreditCard, FilePlus2, FileText, Gauge, Headphones, History, LayoutDashboard,
  LogOut, Menu, Plus, Search, Settings2, ShieldCheck, Sparkles, Users, Workflow, X
} from 'lucide-react'
import api, { friendlyError, setToken } from './api'
import { DEMO_USERS, money } from './data'
import {
  Dashboard, ApplyLoan, MyLoans, LoanDetails, Approvals, RecoveryHistory, Support, SupportDesk, AdminWorkflows, Login
} from './pages'

const nav = [
  { label: 'Workspace', items: [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/apply', label: 'Apply for a loan', icon: FilePlus2 },
    { to: '/loans', label: 'My applications', icon: FileText }
  ]},
  { label: 'Operations', items: [
    { to: '/approvals', label: 'Approval desk', icon: ClipboardCheck, roles: ['DDO','HOO','ADMIN'] },
    { to: '/recoveries', label: 'Recovery history', icon: History, roles: ['EMPLOYEE','DDO','HOO','ADMIN'] },
    { to: '/support', label: 'Support centre', icon: Headphones },
    { to: '/support/desk', label: 'Support staff desk', icon: ClipboardCheck, roles: ['SUPPORT','ADMIN'] }
  ]},
  { label: 'Administration', items: [
    { to: '/admin/workflows', label: 'Approval workflows', icon: Workflow, roles: ['ADMIN'] }
  ]}
]

export default function App() {
  const [demoUsers, setDemoUsers] = useState(DEMO_USERS)
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('loan_demo_user')) || DEMO_USERS[0] } catch { return DEMO_USERS[0] }
  })
  const [menuOpen, setMenuOpen] = useState(false)
  const [demoMode, setDemoMode] = useState(import.meta.env.VITE_DEMO_MODE !== 'false')
  const location = useLocation()
  const navigate = useNavigate()
  const pageTitle = useMemo(() => {
    const item = nav.flatMap(group => group.items).find(x => x.to === location.pathname)
    return item?.label || 'Loan details'
  }, [location.pathname])

  useEffect(() => { localStorage.setItem('loan_demo_user', JSON.stringify(user)) }, [user])

  // Load real database IDs so the demo identity switcher always matches MySQL.
  useEffect(() => {
    let mounted = true
    api.get('/api/demo/users')
      .then(({ data }) => {
        if (!mounted || !Array.isArray(data) || data.length === 0) return
        setDemoUsers(data)
        setUser(current => data.find(item => item.username === current.username) || data[0])
      })
      .catch(error => console.warn('Could not load demo users from backend:', error))
    return () => { mounted = false }
  }, [])

  const visibleNav = nav.map(group => ({ ...group, items: group.items.filter(item => !item.roles || item.roles.includes(user.role)) })).filter(group => group.items.length)

  function changeUser(id) {
    const next = demoUsers.find(x => x.id === Number(id))
    if (next) { setUser(next); navigate('/') }
  }
  function logout() { setToken(null); navigate('/login') }

  return <div className="app-shell">
    <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
      <div className="brand"><div className="brand-mark"><CreditCard size={21}/></div><div><b>LoanDesk</b><small>EMPLOYEE PORTAL</small></div><button className="icon-btn mobile-close" onClick={() => setMenuOpen(false)}><X size={18}/></button></div>
      <div className="workspace-chip"><div className="workspace-icon">W</div><div><b>Workplace</b><small>West Bengal · Finance</small></div><ChevronDown size={15}/></div>
      <div className="nav-groups">{visibleNav.map(group => <section key={group.label}><div className="nav-label">{group.label}</div>{group.items.map(item => { const Icon=item.icon; return <NavLink end={item.to === '/'} onClick={() => setMenuOpen(false)} className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`} to={item.to} key={item.to}><Icon size={18}/><span>{item.label}</span></NavLink>})}</section>)}</div>
      <div className="sidebar-bottom"><div className="help-card"><div className="help-icon"><CircleHelp size={18}/></div><b>Need a hand?</b><p>Our support team is here to help with your application.</p><NavLink to="/support">Visit support <ArrowUpRight size={13}/></NavLink></div><div className="sidebar-foot"><span className="online-dot"/> All systems operational <span>v1.0</span></div></div>
    </aside>
    {menuOpen && <button className="mobile-scrim" onClick={() => setMenuOpen(false)} aria-label="Close navigation"/>}
    <main className="main-area">
      <header className="topbar"><div className="topbar-left"><button className="icon-btn mobile-menu" onClick={() => setMenuOpen(true)}><Menu size={20}/></button><span className="breadcrumb">Portal <span>/</span> {pageTitle}</span></div><div className="topbar-right">
        <div className="demo-label"><span className="online-dot"/> {demoMode ? 'LOCAL DEMO' : 'CONNECTED'}</div>
        <button className="icon-btn notification-btn" title="Notifications"><Bell size={18}/><i/></button>
        <div className="user-menu"><div className="avatar">{user.name.split(' ').map(x=>x[0]).slice(0,2).join('')}</div><div className="user-meta"><b>{user.name}</b><small>{user.role}{user.serviceGroup ? ` · Group ${user.serviceGroup}` : ''}</small></div><select aria-label="Switch demo user" value={user.id} onChange={e=>changeUser(e.target.value)} title="Demo identity selector">{demoUsers.map(u=><option key={u.id} value={u.id}>{u.username} · {u.role}{u.serviceGroup ? ` · Group ${u.serviceGroup}` : ''}</option>)}</select><button className="icon-btn" onClick={logout} title="Sign out"><LogOut size={17}/></button></div>
      </div></header>
      <div className="page-wrap"><Routes>
        <Route path="/login" element={<Login onLogin={token=>{setToken(token);navigate('/')}}/>}/>
        <Route path="/" element={<Dashboard user={user}/>}/>
        <Route path="/apply" element={<ApplyLoan user={user} demoMode={demoMode}/>}/>
        <Route path="/loans" element={<MyLoans user={user}/>}/>
        <Route path="/loans/:loanId" element={<LoanDetails user={user}/>}/>
        <Route path="/approvals" element={<Approvals user={user}/>}/>
        <Route path="/recoveries" element={<RecoveryHistory user={user}/>}/>
        <Route path="/support" element={<Support user={user}/>}/>
        <Route path="/support/desk" element={<SupportDesk user={user}/>}/>
        <Route path="/admin/workflows" element={<AdminWorkflows user={user}/>}/>
        <Route path="*" element={<Navigate to="/" replace/>}/>
      </Routes></div>
      <footer className="page-footer"><span>© {new Date().getFullYear()} LoanDesk</span><span>Built for transparent public service</span><span><ShieldCheck size={13}/> Secure workspace</span></footer>
    </main>
  </div>
}
