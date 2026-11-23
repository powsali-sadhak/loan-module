import React, { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowDownRight, ArrowRight, ArrowUpRight, BadgeCheck, CalendarDays, Check, CheckCircle2,
  ChevronRight, CircleAlert, Clock3, FilePlus2, FileText, Filter, Landmark, LifeBuoy,
  LoaderCircle, MessageSquare, Plus, RefreshCw, Search, Send, ShieldCheck, SlidersHorizontal,
  Sparkles, Wallet, X
} from 'lucide-react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import api, { friendlyError } from './api'
import { badgeClass, dateTime, money, titleCase } from './data'

function PageHeader({eyebrow,title,subtitle,action}) {
  return <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{subtitle}</p></div>{action}</div>
}
function Status({value}) { return <span className={`status-pill ${badgeClass(value)}`}><i/>{titleCase(value)}</span> }
function Spinner(){return <div className="loading"><LoaderCircle className="spin" size={20}/> Loading data…</div>}
function Alert({type='error',children,onClose}) { return <div className={`alert alert-${type}`}>{type==='error'?<CircleAlert size={17}/>:<CheckCircle2 size={17}/>}<span>{children}</span>{onClose&&<button onClick={onClose}><X size={15}/></button>}</div> }
function Empty({title,copy,cta}) { return <div className="empty-state"><div className="empty-icon"><FileText size={24}/></div><b>{title}</b><p>{copy}</p>{cta}</div> }
function Stat({label,value,detail,icon:Icon,tone='blue'}) { return <div className="stat-card"><div className={`stat-icon ${tone}`}><Icon size={19}/></div><div className="stat-label">{label}</div><div className="stat-value">{value}</div><div className="stat-detail">{detail}</div></div> }

export function Dashboard({user}) {
  const [loans,setLoans]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
  async function load(){setLoading(true);setError('');try{const r=await api.get(`/api/loans/user/${user.id}`);setLoans(Array.isArray(r.data)?r.data:[])}catch(e){setError(friendlyError(e))}finally{setLoading(false)}}
  useEffect(()=>{load()},[user.id])
  const pending=loans.filter(x=>['PENDING_APPROVAL','REVISION_REQUESTED'].includes(x.status)).length
  const approved=loans.filter(x=>x.status==='APPROVED').length
  const total=loans.reduce((s,x)=>s+Number(x.amount||0),0)
  const chart=useMemo(()=>{const months=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];return months.map((m,i)=>({month:m,applications:loans.filter(x=>new Date(x.createdAt).getMonth()===i).length}))},[loans])
  return <>
    <PageHeader eyebrow="YOUR WORKSPACE" title={`Good day, ${user.name.split(' ')[0]} 👋`} subtitle="Here’s a snapshot of your loan activity and what needs your attention." action={<button className="btn btn-quiet" onClick={load}><RefreshCw size={16}/> Refresh</button>}/>
    {error&&<Alert>{error}</Alert>}
    <div className="stats-grid"><Stat label="Total applications" value={loading?'—':loans.length} detail="All-time submissions" icon={FileText}/><Stat label="Awaiting action" value={loading?'—':pending} detail="Applications in progress" icon={Clock3} tone="amber"/><Stat label="Approved loans" value={loading?'—':approved} detail="Successfully approved" icon={BadgeCheck} tone="green"/><Stat label="Applied amount" value={loading?'—':money(total)} detail="Across all applications" icon={Wallet} tone="violet"/></div>
    <div className="content-grid"><section className="panel chart-panel"><div className="panel-heading"><div><h3>Application activity</h3><p>Your submissions through the year</p></div><span className="soft-tag">This year</span></div>{loading?<Spinner/>:<div className="chart-wrap"><ResponsiveContainer width="100%" height="100%"><BarChart data={chart}><CartesianGrid vertical={false} stroke="#eef1f5"/><XAxis dataKey="month" axisLine={false} tickLine={false} tick={{fill:'#8a93a3',fontSize:11}}/><YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{fill:'#8a93a3',fontSize:11}}/><Tooltip cursor={{fill:'#f5f7fb'}}/><Bar dataKey="applications" fill="#5068dc" radius={[5,5,0,0]} maxBarSize={28}/></BarChart></ResponsiveContainer></div>}</section>
    <section className="panel quick-panel"><div className="panel-heading"><div><h3>Quick actions</h3><p>Get things moving</p></div><Sparkles size={18} className="muted-icon"/></div><Link className="quick-link" to="/apply"><span className="quick-ico blue"><FilePlus2 size={18}/></span><span><b>Apply for a loan</b><small>Start a new application</small></span><ChevronRight size={17}/></Link><Link className="quick-link" to="/loans"><span className="quick-ico purple"><FileText size={18}/></span><span><b>Track an application</b><small>Review status and history</small></span><ChevronRight size={17}/></Link><Link className="quick-link" to="/support"><span className="quick-ico green"><LifeBuoy size={18}/></span><span><b>Get support</b><small>Raise a service request</small></span><ChevronRight size={17}/></Link></section></div>
    <section className="panel table-panel"><div className="panel-heading"><div><h3>Recent applications</h3><p>A quick look at your latest loan requests</p></div><Link className="text-link" to="/loans">View all <ArrowUpRight size={14}/></Link></div><LoanTable loans={loans.slice(0,5)} loading={loading}/></section>
  </>
}

function LoanTable({loans,loading}) {
  if(loading)return <Spinner/>
  if(!loans.length)return <Empty title="No applications yet" copy="Once you submit a loan request, it will show up here." cta={<Link className="btn btn-primary" to="/apply">Apply for a loan <ArrowRight size={15}/></Link>}/>
  return <div className="table-scroll"><table><thead><tr><th>APPLICATION</th><th>AMOUNT</th><th>PURPOSE</th><th>SUBMITTED</th><th>STATUS</th><th/></tr></thead><tbody>{loans.map(l=><tr key={l.loanId}><td><Link className="id-link" to={`/loans/${l.loanId}`}>LN-{String(l.loanId).padStart(5,'0')}</Link></td><td className="amount-cell">{money(l.amount)}</td><td className="purpose-cell">{l.purpose}</td><td>{dateTime(l.createdAt)}</td><td><Status value={l.status}/></td><td><Link className="table-arrow" to={`/loans/${l.loanId}`}><ChevronRight size={17}/></Link></td></tr>)}</tbody></table></div>
}

export function ApplyLoan({user,demoMode}) {
  const [form,setForm]=useState({amount:'',purpose:''}),[busy,setBusy]=useState(false),[error,setError]=useState(''),[done,setDone]=useState(null)
  const [eligibility,setEligibility]=useState(null),[eligibilityLoading,setEligibilityLoading]=useState(true),[eligibilityError,setEligibilityError]=useState('')
  const nav=useNavigate()

  useEffect(()=>{
    let active=true
    setEligibility(null)
    setEligibilityLoading(true)
    setEligibilityError('')
    api.get(`/api/loans/eligibility/${user.id}`)
      .then(({data})=>{if(active)setEligibility(data)})
      .catch(e=>{if(active)setEligibilityError(friendlyError(e))})
      .finally(()=>{if(active)setEligibilityLoading(false)})
    return ()=>{active=false}
  },[user.id])

  function change(e){setForm({...form,[e.target.name]:e.target.value})}
  const availableAmount=Number(eligibility?.availableAmount||0)
  const maximumApplicationAmount=Math.min(
    Number(eligibility?.maximumApplicationAmount||0),
    availableAmount
  )
  const canApply=Boolean(eligibility)&&!eligibilityLoading&&!eligibilityError&&maximumApplicationAmount>=100000

  async function submit(e){
    e.preventDefault()
    setBusy(true)
    setError('')
    try{
      const body={applicantId:user.id,amount:Number(form.amount),purpose:form.purpose,...(demoMode?{appliedById:user.id}:{})}
      const r=await api.post('/api/loans/apply',body)
      setDone(r.data)
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  if(done)return <div className="success-screen"><div className="success-mark"><Check size={30}/></div><div className="eyebrow">APPLICATION SUBMITTED</div><h1>You’re all set.</h1><p>Your loan request has been sent into the approval workflow.</p><div className="success-summary"><span>Application reference</span><b>LN-{String(done.loanId).padStart(5,'0')}</b><span>Requested amount</span><b>{money(done.amount)}</b></div><button className="btn btn-primary" onClick={()=>nav(`/loans/${done.loanId}`)}>View application <ArrowRight size={16}/></button><button className="btn btn-quiet" onClick={()=>nav('/apply')}>Submit another</button></div>

  return <><PageHeader eyebrow="LOAN SERVICES" title="Apply for a loan" subtitle="Share a few details and submit your request for approval."/>
    <div className="form-layout">
      <form className="panel form-panel" onSubmit={submit}>
        <div className="form-section-title"><span>01</span><div><b>Loan details</b><small>Tell us what you need and how much.</small></div></div>
        {error&&<Alert>{error}</Alert>}
        {eligibilityError&&<Alert>{eligibilityError}</Alert>}
        {eligibilityLoading?<Spinner/>:eligibility&&<div className="form-note"><ShieldCheck size={17}/><span>
          Group {eligibility.serviceGroup} · Financial year {eligibility.financialYear}. Annual group limit: {money(eligibility.annualLimit)}.
          Applied this FY: {money(eligibility.annualApplied)} · Recovered this FY: {money(eligibility.annualPrincipalRecovered)} · Total outstanding: {money(eligibility.outstandingPrincipal)} · Available now: <b>{money(eligibility.availableAmount)}</b>.
          {eligibility.recoveryThresholdMet?' Recovery threshold is met.':' At least 70% principal recovery is required before used entitlement can be replenished.'}
        </span></div>}
        {!eligibilityLoading&&eligibility&&maximumApplicationAmount<100000&&<Alert>{eligibility.message}</Alert>}
        <label className="field"><span>Loan amount <em>*</em></span><div className="input-prefix"><span>₹</span><input type="number" min="100000" max={maximumApplicationAmount||undefined} step="1" required name="amount" value={form.amount} onChange={change} placeholder="Minimum ₹1,00,000" disabled={!canApply}/></div><small>Minimum ₹1,00,000. Maximum for your group/application and current available annual entitlement: {money(maximumApplicationAmount)}.</small></label>
        <label className="field"><span>Purpose of loan <em>*</em></span><textarea required name="purpose" value={form.purpose} onChange={change} rows="4" maxLength="500" placeholder="Briefly explain why you need this loan…" disabled={!canApply}/><small>{form.purpose.length}/500 characters</small></label>
        <div className="form-note"><ShieldCheck size={17}/><span>Applications are reviewed by a DDO and then an HOO. DDO/HOO applicants must be reviewed by a different officer at the relevant stage.</span></div>
        <div className="form-actions"><button type="button" className="btn btn-quiet" onClick={()=>setForm({amount:'',purpose:''})}>Clear form</button><button disabled={busy||!canApply} className="btn btn-primary">{busy?<LoaderCircle className="spin" size={16}/>:<Send size={16}/>} Submit application</button></div>
      </form>
      <aside className="side-info"><div className="info-illustration"><div className="info-orbit orbit-one"/><div className="info-orbit orbit-two"/><div className="info-doc"><FileText size={32}/><CheckCircle2 size={18}/></div></div><h3>Simple. Transparent. Trackable.</h3><p>Each application must be at least ₹1 lakh. Group limits are A/B: ₹10 lakh, C: ₹7 lakh and D: ₹5 lakh per financial year, subject to the 70% principal recovery rule.</p><div className="info-points"><div><CheckCircle2 size={16}/> Track approval progress</div><div><CheckCircle2 size={16}/> View action history</div><div><CheckCircle2 size={16}/> Respond to revision requests</div></div></aside>
    </div>
  </>
}

export function MyLoans({user}) {
  const [loans,setLoans]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[query,setQuery]=useState(''),[filter,setFilter]=useState('ALL')
  async function load(){setLoading(true);try{const r=await api.get(`/api/loans/user/${user.id}`);setLoans(Array.isArray(r.data)?r.data:[]);setError('')}catch(e){setError(friendlyError(e))}finally{setLoading(false)}}
  useEffect(()=>{load()},[user.id])
  const shown=loans.filter(l=>(filter==='ALL'||l.status===filter)&&(`LN-${l.loanId} ${l.purpose} ${l.status}`.toLowerCase().includes(query.toLowerCase())))
  return <><PageHeader eyebrow="LOAN SERVICES" title="My applications" subtitle="All your loan requests, in one place." action={<Link className="btn btn-primary" to="/apply"><Plus size={16}/> New application</Link>}/>{error&&<Alert>{error}</Alert>}<section className="panel table-panel"><div className="filter-bar"><div className="search-field"><Search size={16}/><input placeholder="Search applications…" value={query} onChange={e=>setQuery(e.target.value)}/></div><select className="select-control" value={filter} onChange={e=>setFilter(e.target.value)}><option value="ALL">All statuses</option>{['PENDING_APPROVAL','APPROVED','REJECTED','REVISION_REQUESTED'].map(s=><option key={s} value={s}>{titleCase(s)}</option>)}</select><button className="btn btn-quiet btn-sm" onClick={load}><RefreshCw size={15}/> Refresh</button></div><LoanTable loans={shown} loading={loading}/></section></>
}

export function LoanDetails({user}) {
  const {loanId}=useParams(),[loan,setLoan]=useState(null),[actions,setActions]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('')
  async function load(){setLoading(true);try{const [a,b]=await Promise.all([api.get(`/api/loans/${loanId}`),api.get(`/api/loans/${loanId}/actions`)]);setLoan(a.data);setActions(Array.isArray(b.data)?b.data:[]);setError('')}catch(e){setError(friendlyError(e))}finally{setLoading(false)}}
  useEffect(()=>{load()},[loanId])
  if(loading)return <Spinner/>
  if(error)return <><PageHeader eyebrow="LOAN SERVICES" title="Application details" subtitle="Review your application and its progress."/><Alert>{error}</Alert></>
  if(!loan)return null
  return <><PageHeader eyebrow="APPLICATION LN-{loanId}" title="Application details" subtitle="Review the current status and every recorded action." action={<Link className="btn btn-quiet" to="/loans">Back to applications</Link>}/><div className="detail-grid"><section className="panel detail-main"><div className="detail-top"><div><span className="muted-label">REQUESTED AMOUNT</span><div className="detail-amount">{money(loan.amount)}</div></div><Status value={loan.status}/></div><div className="detail-kv"><div><small>Application ID</small><b>LN-{String(loan.loanId).padStart(5,'0')}</b></div><div><small>Submitted on</small><b>{dateTime(loan.createdAt)}</b></div><div><small>Current owner</small><b>{loan.currentOwner||'Completed'}</b></div><div><small>Current approval role</small><b>{loan.currentApproverRole||'—'}</b></div></div><div className="detail-purpose"><small>LOAN PURPOSE</small><p>{loan.purpose}</p></div></section><section className="panel timeline-panel"><div className="panel-heading"><div><h3>Activity timeline</h3><p>Approval journey and updates</p></div><ActivityIcon/></div>{actions.length?<div className="timeline">{actions.map(a=><div className="timeline-item" key={a.actionId}><div className="timeline-dot"><Check size={13}/></div><div><b>{titleCase(a.actionType)}</b><small>{a.actionBy||'System'} · {dateTime(a.actionTime)}</small>{a.remarks&&<p>{a.remarks}</p>}</div></div>)}</div>:<Empty title="No actions recorded" copy="Updates will appear here as your application moves through review."/>}</section></div></>
}
function ActivityIcon(){return <Clock3 size={18} className="muted-icon"/>}

export function Approvals({user}) {
  const [id,setId]=useState(''),[loan,setLoan]=useState(null),[actions,setActions]=useState([]),[remarks,setRemarks]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('')
  const [repayments,setRepayments]=useState([]),[repaymentAmount,setRepaymentAmount]=useState(''),[repaymentRemarks,setRepaymentRemarks]=useState('')
  const [eligibilitySnapshot,setEligibilitySnapshot]=useState(null)

  async function find(e){
    e?.preventDefault()
    if(!id)return
    setBusy(true);setError('');setSuccess('')
    try{
      const [l,a]=await Promise.all([
        api.get(`/api/loans/${id}`),
        api.get(`/api/loans/${id}/actions`)
      ])
      setLoan(l.data)
      setActions(a.data||[])
      try{
        const eligibilityResponse=await api.get(`/api/loans/eligibility/${l.data.applicantId}`)
        setEligibilitySnapshot(eligibilityResponse.data)
      }catch{setEligibilitySnapshot(null)}
      if(l.data.status==='APPROVED'){
        const repaymentResponse=await api.get(`/api/loans/${id}/repayments`)
        setRepayments(repaymentResponse.data||[])
      }else{
        setRepayments([])
      }
    }catch(e){setError(friendlyError(e));setLoan(null);setRepayments([]);setEligibilitySnapshot(null)}
    finally{setBusy(false)}
  }

  async function act(actionType){
    setBusy(true);setError('');setSuccess('')
    try{
      await api.post('/api/loans/action',{loanId:Number(id),userId:user.id,actionType,remarks})
      const message=`Action ${titleCase(actionType).toLowerCase()} successfully.`
      await find()
      setSuccess(message)
      setRemarks('')
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  async function recordRepayment(e){
    e.preventDefault()
    setBusy(true);setError('');setSuccess('')
    try{
      await api.post(`/api/loans/${id}/repayments`,{
        recordedById:user.id,
        amount:Number(repaymentAmount),
        remarks:repaymentRemarks
      })
      await find()
      setSuccess('Principal repayment recorded successfully.')
      setRepaymentAmount('')
      setRepaymentRemarks('')
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  const totalRecovered=repayments.reduce((sum,item)=>sum+Number(item.amount||0),0)
  const recoveryPercent=loan?.amount?Math.min(100,(totalRecovered/Number(loan.amount))*100):0

  return <>
    <PageHeader eyebrow="OPERATIONS" title="Approval desk" subtitle="Review applications, record approval decisions and maintain principal recovery records."/>
    <div className="panel lookup-panel"><form className="lookup-form" onSubmit={find}><label className="field"><span>Loan application ID</span><input type="number" min="1" required value={id} onChange={e=>setId(e.target.value)} placeholder="Enter numeric loan ID"/></label><button className="btn btn-primary" disabled={busy}>{busy?<LoaderCircle className="spin" size={16}/>:<Search size={16}/>} Find application</button></form></div>
    {error&&<Alert>{error}</Alert>}
    {success&&<Alert type="success">{success}</Alert>}
    {loan&&<>
      <div className="detail-grid">
        <section className="panel detail-main">
          <div className="detail-top"><div><span className="muted-label">REQUESTED AMOUNT</span><div className="detail-amount">{money(loan.amount)}</div></div><Status value={loan.status}/></div>
          <div className="detail-kv">
            <div><small>Application</small><b>LN-{loan.loanId}</b></div>
            <div><small>Applicant</small><b>{loan.applicantName||`User ${loan.applicantId}`}</b></div>
            <div><small>Applicant group</small><b>{loan.applicantGroup||'—'}</b></div>
            <div><small>Current owner</small><b>{loan.currentOwner||'—'}</b></div>
            <div><small>Required role</small><b>{loan.currentApproverRole||'—'}</b></div>
            <div><small>Current step</small><b>{loan.currentStepOrder||'—'}</b></div>
          </div>
          <div className="detail-purpose"><small>PURPOSE</small><p>{loan.purpose}</p></div>
          {loan.status==='PENDING_APPROVAL'&&<><label className="field"><span>Action remarks</span><textarea rows="3" value={remarks} onChange={e=>setRemarks(e.target.value)} placeholder="Add a note for the applicant or next approver…"/></label><div className="action-buttons"><button disabled={busy||!remarks.trim()} className="btn btn-quiet" onClick={()=>act('REVISION_REQUESTED')}>Request revision</button><button disabled={busy||!remarks.trim()} className="btn btn-danger" onClick={()=>act('REJECTED')}>Reject</button><button disabled={busy||!remarks.trim()} className="btn btn-primary" onClick={()=>act(loan.currentApproverRole==='HOO'?'APPROVED':'FORWARDED')}>{loan.currentApproverRole==='HOO'?'Approve':'Forward'} <ArrowRight size={15}/></button></div></>}
          {loan.status==='REVISION_REQUESTED'&&<Alert>Awaiting the applicant's revised submission.</Alert>}
          {['DDO','HOO'].includes(user.role)&&loan.applicantId===user.id&&<Alert>You are the applicant. Another officer in the approval hierarchy must review this application.</Alert>}
        </section>
        <section className="panel timeline-panel">
          <div className="panel-heading"><div><h3>Application history</h3><p>Recorded actions</p></div><Clock3 size={18}/></div>
          {actions.length?<div className="timeline">{actions.map(a=><div className="timeline-item" key={a.actionId}><div className="timeline-dot"><Check size={13}/></div><div><b>{titleCase(a.actionType)}</b><small>{a.actionBy} · {dateTime(a.actionTime)}</small><p>{a.remarks}</p></div></div>)}</div>:<Empty title="No actions recorded" copy="No officer action has been recorded yet."/>}
        </section>
      </div>
      {loan.status==='APPROVED'&&<section className="panel table-panel" style={{marginTop:18}}>
        <div className="panel-heading"><div><h3>Principal recovery</h3><p>Record repayments against this approved loan</p></div><Status value={eligibilitySnapshot?.recoveryThresholdMet?'70% FY RECOVERY MET':'70% FY RECOVERY PENDING'}/></div>
        <div className="detail-kv">
          <div><small>Original principal</small><b>{money(loan.amount)}</b></div>
          <div><small>Recovered on this loan</small><b>{money(totalRecovered)}</b></div>
          <div><small>Outstanding on this loan</small><b>{money(Math.max(0,Number(loan.amount)-totalRecovered))}</b></div>
          <div><small>This loan recovered</small><b>{recoveryPercent.toFixed(1)}%</b></div>
          <div><small>Current FY principal used</small><b>{money(eligibilitySnapshot?.annualApplied)}</b></div>
          <div><small>Total principal recovered</small><b>{money(eligibilitySnapshot?.totalPrincipalRecovered)}</b></div>
          <div><small>Total outstanding across loans</small><b>{money(eligibilitySnapshot?.outstandingPrincipal)}</b></div>
        </div>
        <form className="ticket-form" onSubmit={recordRepayment}>
          <div className="form-grid">
            <label className="field"><span>Principal repayment amount</span><input required type="number" min="1" step="1" value={repaymentAmount} onChange={e=>setRepaymentAmount(e.target.value)} placeholder="Amount received in INR"/></label>
            <label className="field"><span>Remarks / reference</span><input value={repaymentRemarks} onChange={e=>setRepaymentRemarks(e.target.value)} placeholder="Salary recovery / receipt reference"/></label>
          </div>
          <div className="form-actions"><button className="btn btn-primary" disabled={busy||!repaymentAmount||Number(repaymentAmount)<=0||loan.applicantId===user.id}><Check size={15}/> Record repayment</button></div>
        </form>
        {repayments.length>0&&<div className="ticket-list">{repayments.map(item=><div className="ticket-row" key={item.repaymentId}><div className="ticket-symbol"><Wallet size={18}/></div><div className="ticket-main"><b>{money(item.amount)}</b><small>{dateTime(item.receivedAt)} · Recorded by {item.recordedBy||'Officer'}</small></div></div>)}</div>}
        <div className="form-note"><ShieldCheck size={17}/><span>The 70% re-application test is measured against the applicant's aggregate principal usage for the current financial year, not just this individual loan. Only DDO, HOO or ADMIN users other than the borrower can record repayments.</span></div>
      </section>}
    </>}
  </>
}

export function Support({user}) {
  const [tickets,setTickets]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[showForm,setShowForm]=useState(false),[form,setForm]=useState({category:'OTHER',subject:'',description:'',priority:'MEDIUM',loanId:''})
  async function load(){setLoading(true);try{const r=await api.get('/api/support/tickets/my');setTickets(Array.isArray(r.data)?r.data:[]);setError('')}catch(e){setError(friendlyError(e))}finally{setLoading(false)}}
  useEffect(()=>{load()},[])
  async function create(e){e.preventDefault();try{const body={...form,loanId:form.loanId?Number(form.loanId):null};await api.post('/api/support/tickets',body);setShowForm(false);setForm({category:'OTHER',subject:'',description:'',priority:'MEDIUM',loanId:''});load()}catch(e){setError(friendlyError(e))}}
  const staff=['SUPPORT','ADMIN'].includes(user.role)
  return <><PageHeader eyebrow="HELP & SUPPORT" title="Support centre" subtitle="Get help with your loan applications, account or portal access." action={<button className="btn btn-primary" onClick={()=>setShowForm(!showForm)}><Plus size={16}/> Raise a ticket</button>}/>{error&&<Alert>{error}</Alert>}{showForm&&<form className="panel ticket-form" onSubmit={create}><div className="panel-heading"><div><h3>New support request</h3><p>Our team will get back to you through this ticket.</p></div><button type="button" className="icon-btn" onClick={()=>setShowForm(false)}><X size={17}/></button></div><div className="form-grid"><label className="field"><span>Category</span><select value={form.category} onChange={e=>setForm({...form,category:e.target.value})}>{['LOGIN_ACCESS','EMPLOYEE_DETAILS','LOAN_SUBMISSION','ELIGIBILITY','DOCUMENT_UPLOAD','APPLICATION_STATUS','OTHER'].map(x=><option key={x} value={x}>{titleCase(x)}</option>)}</select></label><label className="field"><span>Priority</span><select value={form.priority} onChange={e=>setForm({...form,priority:e.target.value})}>{['LOW','MEDIUM','HIGH','URGENT'].map(x=><option key={x}>{x}</option>)}</select></label><label className="field full"><span>Subject</span><input required value={form.subject} onChange={e=>setForm({...form,subject:e.target.value})} placeholder="What do you need help with?"/></label><label className="field"><span>Loan ID (optional)</span><input type="number" value={form.loanId} onChange={e=>setForm({...form,loanId:e.target.value})} placeholder="e.g. 12"/></label><label className="field full"><span>Description</span><textarea required rows="3" value={form.description} onChange={e=>setForm({...form,description:e.target.value})} placeholder="Describe the issue in a little more detail…"/></label></div><div className="form-actions"><button className="btn btn-primary"><Send size={15}/> Submit ticket</button></div></form>}<section className="panel table-panel"><div className="panel-heading"><div><h3>{staff?'Your tickets':'My support tickets'}</h3><p>Track the status of each service request</p></div><button className="btn btn-quiet btn-sm" onClick={load}><RefreshCw size={15}/> Refresh</button></div>{loading?<Spinner/>:tickets.length?<div className="ticket-list">{tickets.map(t=><div className="ticket-row" key={t.ticketId}><div className="ticket-symbol"><MessageSquare size={18}/></div><div className="ticket-main"><b>{t.subject}</b><small>{t.ticketNumber||`Ticket #${t.ticketId}`} · {titleCase(t.category)} · {dateTime(t.createdAt)}</small></div><Status value={t.status}/><span className="priority">{titleCase(t.priority)}</span></div>)}</div>:<Empty title="No support tickets" copy="Need a hand? Raise a ticket and our support team will take it from there."/>}</section></>
}

export function AdminWorkflows({user}) {
  const [workflows,setWorkflows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[formOpen,setFormOpen]=useState(false),[form,setForm]=useState({name:'',description:'',steps:[{stepOrder:1,approverRole:'DDO',label:'DDO Verification'},{stepOrder:2,approverRole:'HOO',label:'HOO Final Approval'}]})
  async function load(){setLoading(true);try{const r=await api.get('/api/admin/workflows');setWorkflows(Array.isArray(r.data)?r.data:[]);setError('')}catch(e){setError(friendlyError(e))}finally{setLoading(false)}}
  useEffect(()=>{load()},[])
  function stepChange(i,key,value){setForm({...form,steps:form.steps.map((s,n)=>n===i?{...s,[key]:key==='stepOrder'?Number(value):value}:s)})}
  async function create(e){e.preventDefault();try{await api.post('/api/admin/workflows',{...form,active:false});setFormOpen(false);load()}catch(e){setError(friendlyError(e))}}
  async function activate(id){try{await api.post(`/api/admin/workflows/${id}/activate`);load()}catch(e){setError(friendlyError(e))}}
  return <><PageHeader eyebrow="ADMINISTRATION" title="Approval workflows" subtitle="Design and manage the approval path for new loan applications." action={<button className="btn btn-primary" onClick={()=>setFormOpen(!formOpen)}><Plus size={16}/> Create workflow</button>}/>{error&&<Alert>{error}</Alert>}{formOpen&&<form className="panel workflow-form" onSubmit={create}><div className="panel-heading"><div><h3>New workflow</h3><p>Set the steps and the roles responsible for each review.</p></div><button type="button" className="icon-btn" onClick={()=>setFormOpen(false)}><X size={17}/></button></div><label className="field"><span>Workflow name</span><input required value={form.name} onChange={e=>setForm({...form,name:e.target.value})} placeholder="e.g. Standard employee loan"/></label><label className="field"><span>Description</span><input value={form.description} onChange={e=>setForm({...form,description:e.target.value})} placeholder="Short summary of this approval route"/></label><div className="steps-editor"><div className="steps-heading">APPROVAL STEPS <button type="button" className="text-link" onClick={()=>setForm({...form,steps:[...form.steps,{stepOrder:form.steps.length+1,approverRole:'DDO',label:''}]})}><Plus size={14}/> Add step</button></div>{form.steps.map((s,i)=><div className="step-row" key={i}><span className="step-number">{i+1}</span><label className="field"><span>Approver role</span><select value={s.approverRole} onChange={e=>stepChange(i,'approverRole',e.target.value)}><option>DDO</option><option>HOO</option></select></label><label className="field"><span>Step label</span><input required value={s.label} onChange={e=>stepChange(i,'label',e.target.value)} placeholder="Review label"/></label>{form.steps.length>1&&<button type="button" className="icon-btn" onClick={()=>setForm({...form,steps:form.steps.filter((_,n)=>n!==i).map((v,n)=>({...v,stepOrder:n+1}))})}><X size={16}/></button>}</div>)}</div><button className="btn btn-primary"><Check size={15}/> Save workflow</button></form>}<div className="workflow-list">{loading?<Spinner/>:workflows.map(w=><section className="panel workflow-card" key={w.id||w.workflowId}><div className="workflow-card-head"><div><div className="workflow-title">{w.name}</div><p>{w.description||'No description provided'}</p></div><Status value={w.active?'ACTIVE':'INACTIVE'}/></div><div className="workflow-steps">{(w.steps||[]).sort((a,b)=>a.stepOrder-b.stepOrder).map(s=><div className="workflow-step" key={s.stepOrder}><div className="step-number">{s.stepOrder}</div><div><b>{s.label}</b><small>{titleCase(s.approverRole)}</small></div>{s.stepOrder!==(w.steps||[]).length&&<div className="step-connector"/>}</div>)}</div>{!w.active&&<div className="workflow-card-foot"><button className="btn btn-quiet btn-sm" onClick={()=>activate(w.id||w.workflowId)}>Set as active <ArrowRight size={14}/></button></div>}</section>)}</div>{!loading&&!workflows.length&&<Empty title="No workflows found" copy="Create your first approval workflow to route incoming applications."/>}</>
}

export function Login({onLogin}) {
  const [username,setUsername]=useState('admin'),[password,setPassword]=useState(''),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  async function submit(e){e.preventDefault();setBusy(true);setError('');try{const r=await api.post('/api/auth/login',{username,password});const token=typeof r.data==='string'?r.data:r.data.token||r.data.accessToken;if(!token)throw new Error('Login response did not contain a JWT token.');onLogin(token)}catch(e){setError(friendlyError(e))}finally{setBusy(false)}}
  return <div className="login-screen"><div className="login-card"><div className="brand login-brand"><div className="brand-mark"><Landmark size={21}/></div><div><b>LoanDesk</b><small>EMPLOYEE PORTAL</small></div></div><div className="eyebrow">WELCOME BACK</div><h1>Sign in to your workspace</h1><p>Use your registered account to access loan services.</p>{error&&<Alert>{error}</Alert>}<form onSubmit={submit}><label className="field"><span>Username</span><input required value={username} onChange={e=>setUsername(e.target.value)} autoComplete="username"/></label><label className="field"><span>Password</span><input required type="password" value={password} onChange={e=>setPassword(e.target.value)} autoComplete="current-password"/></label><button className="btn btn-primary btn-block" disabled={busy}>{busy?<LoaderCircle className="spin" size={16}/>:null} Sign in</button></form><small className="login-foot"><ShieldCheck size={14}/> Your session is protected by JWT authentication.</small></div></div>
}