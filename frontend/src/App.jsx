import React, { useEffect, useMemo, useState } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import {
  Activity, ArrowDownLeft, ArrowUpRight, Bell, BriefcaseBusiness, ChevronDown, CircleHelp, MessageCircle,
  ClipboardCheck, CreditCard, FilePlus2, FileText, Gauge, Headphones, History, LayoutDashboard,
  LogOut, Menu, Plus, Search, Settings2, ShieldCheck, Sparkles, Users, Workflow, X
} from 'lucide-react'
import api, { friendlyError, setToken } from './api'
import { DEMO_USERS, dateTime } from './data'
import {
  Dashboard, ApplyLoan, MyLoans, LoanDetails, Approvals, RecoveryHistory, Support, SupportDesk, AdminWorkflows, Login
} from './pages'

const SUPPORT_ONLY_ROLES = ['SUPPORT', 'ADMIN']
const LOAN_USER_ROLES = ['EMPLOYEE', 'DDO', 'HOO']

const nav = [
  { label: 'Workspace', items: [
    { to: '/', label: 'Overview', icon: LayoutDashboard, roles: LOAN_USER_ROLES },
    { to: '/apply', label: 'Apply for a loan', icon: FilePlus2, roles: LOAN_USER_ROLES },
    { to: '/loans', label: 'My applications', icon: FileText, roles: LOAN_USER_ROLES }
  ]},
  { label: 'Operations', items: [
    { to: '/approvals', label: 'Approval desk', icon: ClipboardCheck, roles: ['DDO','HOO'] },
    { to: '/recoveries', label: 'Recovery history', icon: History, roles: LOAN_USER_ROLES },
    { to: '/support', label: 'Support centre', icon: Headphones },
    { to: '/support/desk', label: 'Support staff desk', icon: ClipboardCheck, roles: SUPPORT_ONLY_ROLES }
  ]}
]

export default function App() {
  const [demoUsers, setDemoUsers] = useState(DEMO_USERS)
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('loan_demo_user')) || DEMO_USERS[0] } catch { return DEMO_USERS[0] }
  })
  const [menuOpen, setMenuOpen] = useState(false)
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [notificationsLoading, setNotificationsLoading] = useState(false)
  const [whatsAppOpen, setWhatsAppOpen] = useState(false)
  const [whatsAppNumber, setWhatsAppNumber] = useState(user.mobileNumber || '')
  const [whatsAppEnabled, setWhatsAppEnabled] = useState(Boolean(user.whatsappNotificationsEnabled))
  const [whatsAppSaving, setWhatsAppSaving] = useState(false)
  const [whatsAppStatus, setWhatsAppStatus] = useState('')
  const [demoMode, setDemoMode] = useState(import.meta.env.VITE_DEMO_MODE !== 'false')
  const location = useLocation()
  const navigate = useNavigate()
  const pageTitle = useMemo(() => {
    const item = nav.flatMap(group => group.items).find(x => x.to === location.pathname)
    return item?.label || 'Loan details'
  }, [location.pathname])

  useEffect(() => { localStorage.setItem('loan_demo_user', JSON.stringify(user)) }, [user])

  useEffect(() => {
    setWhatsAppNumber(user.mobileNumber || '')
    setWhatsAppEnabled(Boolean(user.whatsappNotificationsEnabled))
    setWhatsAppStatus('')
  }, [user.id, user.mobileNumber, user.whatsappNotificationsEnabled])

  async function loadNotifications(showSpinner = false) {
    if (showSpinner) setNotificationsLoading(true)
    try {
      const [listResponse, countResponse] = await Promise.all([
        api.get('/api/notifications'),
        api.get('/api/notifications/unread-count')
      ])
      setNotifications(Array.isArray(listResponse.data) ? listResponse.data : [])
      setUnreadCount(Number(countResponse.data?.count || 0))
    } catch (error) {
      console.warn('Could not load notifications:', error)
    } finally {
      if (showSpinner) setNotificationsLoading(false)
    }
  }

  useEffect(() => {
    setNotifications([])
    setUnreadCount(0)
    loadNotifications()
    const refreshId = setInterval(() => loadNotifications(), 15000)
    return () => clearInterval(refreshId)
  }, [user.id])

  async function openNotification(item) {
    if (!item.read) {
      try {
        await api.patch('/api/notifications/' + item.id + '/read')
        setNotifications(current => current.map(notification =>
          notification.id === item.id ? { ...notification, read: true, readAt: new Date().toISOString() } : notification
        ))
        setUnreadCount(current => Math.max(0, current - 1))
      } catch (error) {
        console.warn('Could not mark notification as read:', error)
      }
    }
    setNotificationsOpen(false)
    if (item.referenceType === 'LOAN_APPLICATION' && item.referenceId) {
      navigate('/loans/' + item.referenceId)
    } else if (item.referenceType === 'SUPPORT_TICKET') {
      navigate('/support')
    }
  }

  async function markAllNotificationsRead() {
    try {
      await api.patch('/api/notifications/read-all')
      setNotifications(current => current.map(item => ({
        ...item,
        read: true,
        readAt: item.readAt || new Date().toISOString()
      })))
      setUnreadCount(0)
    } catch (error) {
      console.warn('Could not mark all notifications as read:', error)
    }
  }

  async function saveWhatsAppSettings(event) {
    event.preventDefault()
    setWhatsAppSaving(true)
    setWhatsAppStatus('')
    try {
      const { data } = await api.patch(`/api/demo/users/${user.id}/whatsapp-settings`, {
        mobileNumber: whatsAppNumber.trim(),
        whatsappNotificationsEnabled: whatsAppEnabled
      })
      setUser(current => ({ ...current, ...data }))
      setDemoUsers(current => current.map(item => item.id === data.id ? { ...item, ...data } : item))
      setWhatsAppStatus('WhatsApp preferences saved.')
    } catch (error) {
      setWhatsAppStatus(friendlyError(error))
    } finally {
      setWhatsAppSaving(false)
    }
  }

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

  const supportOnly = SUPPORT_ONLY_ROLES.includes(user.role)
  const visibleNav = nav
    .map(group => ({
      ...group,
      items: group.items.filter(item => !item.roles || item.roles.includes(user.role))
    }))
    .filter(group => group.items.length)

  function changeUser(id) {
    const next = demoUsers.find(x => x.id === Number(id))
    if (next) {
      setUser(next)
      navigate(SUPPORT_ONLY_ROLES.includes(next.role) ? '/support/desk' : '/')
    }
  }
  function logout() { setToken(null); navigate('/login') }

  return <div className="app-shell">
    <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
      <div className="brand"><div className="brand-mark"><CreditCard size={21}/></div><div><b>LoanDesk</b><small>EMPLOYEE PORTAL</small></div><button className="icon-btn mobile-close" onClick={() => setMenuOpen(false)}><X size={18}/></button></div>
      <div className="workspace-chip"><div className="workspace-icon">W</div><div><b>Demo workspace</b><small>Portfolio demo · Loan workflow</small></div><ChevronDown size={15}/></div>
      <div className="nav-groups">{visibleNav.map(group => <section key={group.label}><div className="nav-label">{group.label}</div>{group.items.map(item => { const Icon=item.icon; return <NavLink end={item.to === '/'} onClick={() => setMenuOpen(false)} className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`} to={item.to} key={item.to}><Icon size={18}/><span>{item.label}</span></NavLink>})}</section>)}</div>
      <div className="sidebar-bottom"><div className="help-card"><div className="help-icon"><CircleHelp size={18}/></div><b>Need a hand?</b><p>Our support team is here to help with your application.</p><NavLink to="/support">Visit support <ArrowUpRight size={13}/></NavLink></div><div className="sidebar-foot"><span className="online-dot"/> All systems operational <span>v1.0</span></div></div>
    </aside>
    {menuOpen && <button className="mobile-scrim" onClick={() => setMenuOpen(false)} aria-label="Close navigation"/>}
    <main className="main-area">
      <header className="topbar"><div className="topbar-left"><button className="icon-btn mobile-menu" onClick={() => setMenuOpen(true)}><Menu size={20}/></button><span className="breadcrumb">Portal <span>/</span> {pageTitle}</span></div><div className="topbar-right">
        <div className="demo-label"><span className="online-dot"/> {demoMode ? 'LOCAL DEMO' : 'CONNECTED'}</div>
        <div style={{position:'relative'}}>
          <button
            type="button"
            className="icon-btn"
            title="WhatsApp notification settings"
            aria-label="WhatsApp notification settings"
            aria-expanded={whatsAppOpen}
            onClick={() => {
              setWhatsAppOpen(value => !value)
              setNotificationsOpen(false)
              setWhatsAppStatus('')
            }}
          ><MessageCircle size={18}/></button>
          {whatsAppOpen&&<div style={{
            position:'absolute',right:0,top:'calc(100% + 12px)',width:'min(340px, calc(100vw - 28px))',
            background:'#fff',border:'1px solid #e6eaf1',borderRadius:13,
            boxShadow:'0 18px 50px rgba(28,42,72,.18)',zIndex:1000,padding:16
          }}>
            <form onSubmit={saveWhatsAppSettings}>
              <b style={{display:'block',fontSize:13,color:'#26344d'}}>WhatsApp notifications</b>
              <p style={{fontSize:10,color:'#8994a7',lineHeight:1.5,margin:'6px 0 12px'}}>
                Add your WhatsApp number and choose whether this account can receive LoanDesk updates.
              </p>
              <label style={{display:'block',fontSize:10,fontWeight:700,color:'#536178',marginBottom:5}}>Mobile number</label>
              <input
                type="tel"
                value={whatsAppNumber}
                onChange={event=>setWhatsAppNumber(event.target.value)}
                placeholder="+91 98765 43210"
                autoComplete="tel"
                style={{width:'100%',boxSizing:'border-box',padding:'10px 11px',border:'1px solid #dfe4ed',borderRadius:8,fontSize:12}}
              />
              <label style={{display:'flex',alignItems:'flex-start',gap:8,fontSize:11,color:'#536178',lineHeight:1.45,margin:'12px 0'}}>
                <input
                  type="checkbox"
                  checked={whatsAppEnabled}
                  onChange={event=>setWhatsAppEnabled(event.target.checked)}
                  style={{marginTop:2}}
                />
                I agree to receive WhatsApp notifications for this account.
              </label>
              <button type="submit" className="btn btn-primary btn-sm" disabled={whatsAppSaving}>
                {whatsAppSaving?'Saving…':'Save preferences'}
              </button>
              {whatsAppStatus&&<small role="status" style={{display:'block',marginTop:9,fontSize:10,color:whatsAppStatus.includes('saved')?'#27835d':'#bd4b55'}}>{whatsAppStatus}</small>}
            </form>
          </div>}
        </div>
        <div style={{position:'relative'}}>
          <button
            type="button"
            className="icon-btn notification-btn"
            title="Notifications"
            aria-label={'Notifications, '+unreadCount+' unread'}
            aria-expanded={notificationsOpen}
            onClick={() => {
              setNotificationsOpen(value => !value)
              if (!notificationsOpen) loadNotifications(true)
            }}
            style={{position:'relative'}}
          >
            <Bell size={18}/>
            {unreadCount>0&&<span style={{
              position:'absolute',top:-4,right:-4,minWidth:17,height:17,padding:'0 4px',
              borderRadius:20,background:'#df4b58',color:'#fff',fontSize:9,fontWeight:800,
              display:'grid',placeItems:'center',border:'2px solid #fff'
            }}>{unreadCount>99?'99+':unreadCount}</span>}
          </button>
          {notificationsOpen&&<div style={{
            position:'absolute',right:0,top:'calc(100% + 12px)',width:'min(390px, calc(100vw - 28px))',
            background:'#fff',border:'1px solid #e6eaf1',borderRadius:13,
            boxShadow:'0 18px 50px rgba(28,42,72,.18)',zIndex:1000,overflow:'hidden'
          }}>
            <div style={{display:'flex',alignItems:'center',justifyContent:'space-between',gap:12,padding:'14px 16px',borderBottom:'1px solid #edf0f5'}}>
              <div>
                <b style={{display:'block',fontSize:13,color:'#26344d'}}>Notifications</b>
                <small style={{fontSize:10,color:'#8994a7'}}>{unreadCount} unread</small>
              </div>
              <button
                type="button"
                className="btn btn-quiet btn-sm"
                onClick={markAllNotificationsRead}
                disabled={unreadCount===0}
              >Mark all read</button>
            </div>
            <div style={{maxHeight:420,overflowY:'auto'}}>
              {notificationsLoading
                ?<div style={{padding:24,textAlign:'center',fontSize:11,color:'#8994a7'}}>Loading notifications…</div>
                :notifications.length===0
                  ?<div style={{padding:28,textAlign:'center'}}>
                    <Bell size={22} color="#a0aabd"/>
                    <b style={{display:'block',marginTop:8,fontSize:12,color:'#4c5b72'}}>You're all caught up</b>
                    <small style={{fontSize:10,color:'#98a3b5'}}>New loan and support updates will appear here.</small>
                  </div>
                  :notifications.map(item=><button
                    type="button"
                    key={item.id}
                    onClick={()=>openNotification(item)}
                    style={{
                      display:'block',width:'100%',padding:'12px 16px',textAlign:'left',
                      border:0,borderBottom:'1px solid #f0f2f6',
                      background:item.read?'#fff':'#f5f7ff',cursor:'pointer'
                    }}
                  >
                    <span style={{display:'flex',alignItems:'flex-start',gap:9}}>
                      <span style={{width:8,height:8,marginTop:5,flex:'0 0 8px',borderRadius:50,background:item.read?'#dce1e9':'#5068dc'}}/>
                      <span style={{minWidth:0,flex:1}}>
                        <b style={{display:'block',fontSize:11,color:'#34415a',lineHeight:1.4}}>{item.title}</b>
                        <span style={{display:'block',fontSize:10,color:'#66748a',lineHeight:1.5,marginTop:3}}>{item.message}</span>
                        <small style={{display:'block',fontSize:9,color:'#98a3b5',marginTop:5}}>{dateTime(item.createdAt)}</small>
                      </span>
                    </span>
                  </button>)}
            </div>
            <div style={{padding:'9px 16px',background:'#fafbfc',borderTop:'1px solid #edf0f5',display:'flex',justifyContent:'flex-end'}}>
              <button type="button" className="btn btn-quiet btn-sm" onClick={()=>setNotificationsOpen(false)}>Close</button>
            </div>
          </div>}
        </div>
        <div className="user-menu"><div className="avatar">{user.name.split(' ').map(x=>x[0]).slice(0,2).join('')}</div><div className="user-meta"><b>{user.name}</b><small>{user.role}{user.serviceGroup ? ` · Group ${user.serviceGroup}` : ''}</small></div><select aria-label="Switch demo user" value={user.id} onChange={e=>changeUser(e.target.value)} title="Demo identity selector">{demoUsers.map(u=><option key={u.id} value={u.id}>{u.username} · {u.role}{u.serviceGroup ? ` · Group ${u.serviceGroup}` : ''}</option>)}</select><button className="icon-btn" onClick={logout} title="Sign out"><LogOut size={17}/></button></div>
      </div></header>
      <div className="page-wrap"><Routes>
        <Route path="/login" element={<Login onLogin={token=>{setToken(token);navigate('/')}}/>}/>
        <Route path="/" element={supportOnly ? <Navigate to="/support/desk" replace/> : <Dashboard user={user}/>}/>
        <Route path="/apply" element={supportOnly ? <Navigate to="/support/desk" replace/> : <ApplyLoan user={user} demoMode={demoMode}/>}/>
        <Route path="/loans" element={supportOnly ? <Navigate to="/support/desk" replace/> : <MyLoans user={user}/>}/>
        <Route path="/loans/:loanId" element={supportOnly ? <Navigate to="/support/desk" replace/> : <LoanDetails user={user}/>}/>
        <Route path="/approvals" element={supportOnly ? <Navigate to="/support/desk" replace/> : <Approvals user={user}/>}/>
        <Route path="/recoveries" element={supportOnly ? <Navigate to="/support/desk" replace/> : <RecoveryHistory user={user}/>}/>
        <Route path="/support" element={<Support user={user}/>}/>
        <Route path="/support/desk" element={SUPPORT_ONLY_ROLES.includes(user.role) ? <SupportDesk user={user}/> : <Navigate to="/support" replace/>}/>
        <Route path="/admin/workflows" element={<Navigate to={supportOnly ? "/support/desk" : "/"} replace/>}/>
        <Route path="*" element={<Navigate to="/" replace/>}/>
      </Routes></div>
      <footer className="page-footer"><span>© {new Date().getFullYear()} LoanDesk</span><span>Built for transparent public service</span><span><ShieldCheck size={13}/> Secure workspace</span></footer>
    </main>
  </div>
}
