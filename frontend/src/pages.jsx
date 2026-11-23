import React, { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Activity, ArrowDownRight, ArrowRight, ArrowUpRight, BadgeCheck, CalendarDays, Check, CheckCircle2,
  ChevronRight, CircleAlert, Clock3, Download, Eye, FilePlus2, FileText, Filter, Landmark, LifeBuoy, Paperclip,
  Inbox, LoaderCircle, MessageSquare, Plus, RefreshCw, Search, Send, ShieldCheck, SlidersHorizontal, UserRound,
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

function parseLoanAmount(message) {
  const lakh = message.match(/(?:₹\s*)?(\d+(?:\.\d+)?)\s*(?:lakh|lakhs|lac|lacs)\b/i)
  if (lakh) return Math.round(Number(lakh[1]) * 100000)

  const thousand = message.match(/(?:₹\s*)?(\d+(?:\.\d+)?)\s*(?:thousand|k)\b/i)
  if (thousand) return Math.round(Number(thousand[1]) * 1000)

  const rupees = message.match(/(?:₹|rs\.?\s*)(\d[\d,]*)/i)
    || message.match(/\b(\d[\d,]{4,})\b/)
  if (rupees) return Number(rupees[1].replaceAll(',', ''))
  return null
}

function suggestTicket(message) {
  const text = message.toLowerCase()
  let category = 'OTHER'
  let subject = 'General support assistance'

  if (/login|log in|sign in|password|account access|locked out|otp|authentication/.test(text)) {
    category = 'LOGIN_ACCESS'
    subject = 'Login or account access assistance'
  } else if (/document|upload|attachment|file|pdf/.test(text)) {
    category = 'DOCUMENT_UPLOAD'
    subject = 'Document upload assistance'
  } else if (/eligib|limit|group [abcd]|loan amount|recovery|70 percent|70%/.test(text)) {
    category = 'ELIGIBILITY'
    subject = 'Loan eligibility clarification'
  } else if (/status|track|pending|approved|rejected|forwarded|stuck|waiting|not moving/.test(text)) {
    category = 'APPLICATION_STATUS'
    subject = 'Loan application status enquiry'
  } else if (/apply|submit loan|loan submission|form|application submit/.test(text)) {
    category = 'LOAN_SUBMISSION'
    subject = 'Loan application assistance'
  } else if (/employee detail|name change|email|profile|employee record/.test(text)) {
    category = 'EMPLOYEE_DETAILS'
    subject = 'Employee details correction'
  }

  const priority = /urgent|immediately|security breach|fraud|deadline today|salary blocked/.test(text)
    ? 'URGENT'
    : /cannot|can't|unable|blocked|not working|error|failed/.test(text)
      ? 'HIGH'
      : 'MEDIUM'

  return { category, subject, description: message.trim(), priority }
}

function LoanDeskAssistant({mode,user,eligibility,onApply}) {
  const [open,setOpen] = useState(false)
  const [input,setInput] = useState('')
  const [messages,setMessages] = useState([{
    from:'assistant',
    text:mode==='loan'
      ? `Hi ${user.name.split(' ')[0]}! I’m your LoanDesk Guide. Ask me about your Group ${user.serviceGroup||'—'} loan limit, the 70% recovery rule, the approval process, or tell me the amount you need.`
      : `Hi ${user.name.split(' ')[0]}! I can help you describe the issue, choose a ticket category and suggest a priority. Tell me what happened and I’ll prepare a draft for you to review.`
  }])
  const [suggestion,setSuggestion] = useState(null)

  function answerLoanQuestion(message) {
    const text = message.toLowerCase()
    const group = String(eligibility?.serviceGroup||user.serviceGroup||'').toUpperCase()
    const configuredLimit = ({A:1000000,B:1000000,C:700000,D:500000})[group] || 0
    const annualLimit = Number(eligibility?.annualLimit ?? configuredLimit)
    const available = Number(eligibility?.availableAmount ?? 0)
    const maximum = Math.min(
      Number(eligibility?.maximumApplicationAmount ?? annualLimit),
      available
    )
    const amount = parseLoanAmount(message)

    if (/interest rate|interest|emi|instalment|installment|repayment period|tenure/.test(text)) {
      return {
        text:'The current LoanDesk policy data does not specify an interest rate, EMI amount or repayment tenure. Please confirm those details with your Finance/Accounts authority before submitting.',
        suggestion:null
      }
    }

    if (amount !== null) {
      if (!eligibility) {
        const groupCheck = amount > annualLimit
          ? `${money(amount)} is above the Group ${group} ceiling of ${money(annualLimit)}.`
          : amount < 100000
            ? `${money(amount)} is below the minimum application amount of ₹1,00,000.`
            : `${money(amount)} is within the Group ${group} ceiling of ${money(annualLimit)}.`
        return {
          text:groupCheck + ' I cannot confirm your currently available entitlement until your live eligibility information has loaded. Please check the eligibility panel and ask me again.',
          suggestion:null
        }
      }

      const suggestion = amount >= 100000 && amount <= maximum
        ? {amount:String(amount)}
        : null
      let explanation
      if (amount < 100000) {
        explanation = `${money(amount)} is below the minimum application amount of ₹1,00,000.`
      } else if (amount > annualLimit) {
        explanation = `${money(amount)} is above your Group ${group} limit of ${money(annualLimit)} per application.`
      } else if (amount > maximum) {
        explanation = `${money(amount)} is above your currently available entitlement of ${money(maximum)}. Check your previous borrowing and principal recovery.`
      } else {
        explanation = `${money(amount)} is within the currently available limit shown for Group ${group}. You can use this amount in the form; the server will validate eligibility again when you submit.`
      }
      return {text:explanation,suggestion}
    }

    if (/70%|70 percent|recover|repaid|recovery|exhaust/.test(text)) {
      return {
        text:'When the applicable borrowing entitlement has been used, at least 70% of the principal already availed must be recovered before that repayment restores borrowing headroom. The eligibility panel above shows your current recovery and available amount.'
          + (eligibility ? ` Your total principal recovered is ${money(eligibility.totalPrincipalRecovered)} and your outstanding principal is ${money(eligibility.outstandingPrincipal)}.` : ''),
        suggestion:null
      }
    }

    if (/approv|who.*check|who.*review|workflow|ddo|hoo/.test(text)) {
      return {
        text:'The configured approval route is DDO review followed by HOO final approval. An applicant cannot approve their own application. DDO applicants are reviewed by another DDO and then a HOO.',
        suggestion:null
      }
    }

    if (/limit|maximum|how much|group|eligible|eligib/.test(text)) {
      return {
        text:`Your Group ${group||'—'} maximum is ${money(annualLimit)}. The minimum application is ₹1,00,000. Based on the live eligibility response, you currently have ${money(available)} available. The financial year follows 1 April to 31 March.`,
        suggestion:null
      }
    }

    return {
      text:'I can help with your group loan ceiling, the amount currently available, the 70% principal recovery rule, or who approves the application. You can also type an amount such as “I need 2 lakh” and I’ll check it against your displayed eligibility.',
      suggestion:null
    }
  }

  function answerTicketQuestion(message) {
    const draft = suggestTicket(message)
    const categoryName = titleCase(draft.category)
    const priorityName = titleCase(draft.priority)
    return {
      text:`This sounds like a ${categoryName} issue. I suggest ${priorityName} priority. I’ve prepared a ticket subject and description from what you told me. Review the draft before using it; nothing is submitted automatically.`,
      suggestion:draft
    }
  }

  function sendMessage(raw=input) {
    const message=String(raw||'').trim()
    if(!message)return
    const result=mode==='loan'
      ? answerLoanQuestion(message)
      : answerTicketQuestion(message)
    setMessages(current=>[
      ...current,
      {from:'user',text:message},
      {from:'assistant',text:result.text}
    ])
    setSuggestion(result.suggestion)
    setInput('')
  }

  function applySuggestion() {
    if(!suggestion)return
    onApply(suggestion)
    setMessages(current=>[...current,{
      from:'assistant',
      text:mode==='loan'
        ?'I’ve filled the suggested loan amount into your form. Please complete or review the purpose before submitting.'
        :'I’ve filled the suggested ticket details into your form. Please review the subject, description, category and priority before submitting.'
    }])
    setSuggestion(null)
  }

  const prompts = mode==='loan'
    ? ['What is my group limit?','Explain the 70% recovery rule','Who approves my loan?']
    : ['I cannot log in to LoanDesk','My loan application is stuck','I cannot upload a document']

  return <section className="panel" style={{margin:'16px 0',padding:0,overflow:'hidden',border:'1px solid #e7eaf2'}}>
    <button
      type="button"
      onClick={()=>setOpen(value=>!value)}
      style={{width:'100%',display:'flex',alignItems:'center',gap:10,padding:'13px 16px',border:0,background:'#f7f8ff',cursor:'pointer',textAlign:'left'}}
    >
      <span style={{width:34,height:34,borderRadius:10,display:'grid',placeItems:'center',background:'#e9edff',color:'#5068dc'}}><Sparkles size={17}/></span>
      <span style={{flex:1}}>
        <b style={{display:'block',fontSize:12,color:'#34415a'}}>LoanDesk Guide</b>
        <small style={{display:'block',fontSize:9,color:'#8792a5'}}>{mode==='loan'?'Loan eligibility and application help':'Ticket category, priority and request drafting'}</small>
      </span>
      <span style={{fontSize:10,color:'#697791'}}>{open?'Hide assistant':'Chat with assistant'}</span>
      <ChevronRight size={16} style={{transform:open?'rotate(90deg)':'none',transition:'transform .15s'}}/>
    </button>
    {open&&<div style={{padding:14}}>
      <div style={{display:'flex',flexWrap:'wrap',gap:6,marginBottom:10}}>
        {prompts.map(prompt=><button key={prompt} type="button" className="btn btn-quiet btn-sm" onClick={()=>sendMessage(prompt)}>{prompt}</button>)}
      </div>
      <div aria-live="polite" style={{display:'flex',flexDirection:'column',gap:9,maxHeight:250,overflowY:'auto',padding:'4px 2px 12px'}}>
        {messages.map((item,index)=><div key={index} style={{display:'flex',justifyContent:item.from==='user'?'flex-end':'flex-start'}}>
          <div style={{
            maxWidth:'88%',padding:'9px 11px',borderRadius:11,
            background:item.from==='user'?'#edf0ff':'#f4f6f9',
            color:'#445168',fontSize:10,lineHeight:1.55,whiteSpace:'pre-wrap'
          }}>{item.text}</div>
        </div>)}
      </div>
      {suggestion&&<div className="form-note" style={{marginBottom:10}}>
        <Sparkles size={16}/>
        <span>{mode==='loan'?<>Suggested amount: <b>{money(suggestion.amount)}</b></>:<>Suggested ticket: <b>{titleCase(suggestion.category)}</b> · <b>{titleCase(suggestion.priority)}</b></>}</span>
        <button type="button" className="btn btn-quiet btn-sm" onClick={applySuggestion}>Use in form</button>
      </div>}
      <div style={{display:'flex',gap:8,alignItems:'center'}}>
        <input
          aria-label="Message LoanDesk Guide"
          value={input}
          onChange={event=>setInput(event.target.value)}
          onKeyDown={event=>{if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();sendMessage()}}}
          placeholder={mode==='loan'?'Ask about your loan or enter an amount…':'Describe the issue you need help with…'}
          style={{flex:1,minWidth:0,border:'1px solid #e2e7f0',borderRadius:8,padding:'10px 11px',fontSize:11}}
        />
        <button type="button" className="btn btn-primary" onClick={()=>sendMessage()} disabled={!input.trim()} aria-label="Send message"><Send size={14}/></button>
      </div>
      <small style={{display:'block',marginTop:8,fontSize:9,color:'#98a1af'}}>
        {mode==='loan'
          ?'Guidance uses your current eligibility information. Final validation happens when you submit.'
          :'The assistant drafts suggestions only. You remain in control of the ticket before submission.'}
      </small>
    </div>}
  </section>
}

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
          Applied this FY: {money(eligibility.annualApplied)} · Recovered this FY: {money(eligibility.annualPrincipalRecovered)} · Total recovered: {money(eligibility.totalPrincipalRecovered)} · Outstanding: {money(eligibility.outstandingPrincipal)} · Available now: <b>{money(eligibility.availableAmount)}</b>.
          {eligibility.recoveryThresholdMet?' Recovery threshold is met.':' At least 70% principal recovery is required before used entitlement can be replenished.'}
        </span></div>}
        {!eligibilityLoading&&eligibility&&maximumApplicationAmount<100000&&<Alert>{eligibility.message}</Alert>}
        <label className="field"><span>Loan amount <em>*</em></span><div className="input-prefix"><span>₹</span><input type="number" min="100000" max={maximumApplicationAmount||undefined} step="1" required name="amount" value={form.amount} onChange={change} placeholder="Minimum ₹1,00,000" disabled={!canApply}/></div><small>Minimum ₹1,00,000. Maximum for your group/application and current available annual entitlement: {money(maximumApplicationAmount)}.</small></label>
        <label className="field"><span>Purpose of loan <em>*</em></span><textarea required name="purpose" value={form.purpose} onChange={change} rows="4" maxLength="500" placeholder="Briefly explain why you need this loan…" disabled={!canApply}/><small>{form.purpose.length}/500 characters</small></label>
        <div className="form-note"><ShieldCheck size={17}/><span>Applications are reviewed by a DDO and then an HOO. DDO/HOO applicants must be reviewed by a different officer at the relevant stage.</span></div>
        {['EMPLOYEE','DDO'].includes(user.role)&&<LoanDeskAssistant mode="loan" user={user} eligibility={eligibility} onApply={values=>setForm(current=>({...current,...values}))}/>}
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
  return <><PageHeader eyebrow="APPLICATION LN-{loanId}" title="Application details" subtitle="Review the current status and every recorded action." action={<Link className="btn btn-quiet" to="/loans">Back to applications</Link>}/><div className="detail-grid"><section className="panel detail-main"><div className="detail-top"><div><span className="muted-label">REQUESTED AMOUNT</span><div className="detail-amount">{money(loan.amount)}</div></div><Status value={loan.status}/></div><div className="detail-kv"><div><small>Application ID</small><b>LN-{String(loan.loanId).padStart(5,'0')}</b></div><div><small>Submitted on</small><b>{dateTime(loan.createdAt)}</b></div><div><small>Current owner</small><b>{loan.currentOwner||'Completed'}</b></div><div><small>Current approval role</small><b>{loan.currentApproverRole||'—'}</b></div></div><div className="detail-purpose"><small>LOAN PURPOSE</small><p>{loan.purpose}</p></div></section><section className="panel timeline-panel"><div className="panel-heading"><div><h3>Activity timeline</h3><p>Approval journey and updates</p></div><ActivityIcon/></div>{actions.length?<div className="timeline">{actions.map(a=><div className="timeline-item" key={a.actionId}><div className="timeline-dot"><Check size={13}/></div><div><b>{titleCase(a.actionType)}</b><small>{a.actionBy||'System'} · {dateTime(a.actionTime)}</small>{a.remarks&&<p>{a.remarks}</p>}</div></div>)}</div>:<Empty title="No actions recorded" copy="Updates will appear here as your application moves through review."/>}</section></div><LoanDocuments loan={loan} user={user}/></>
}

function formatDocumentSize(bytes) {
  const size = Number(bytes || 0)
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

function LoanDocuments({loan,user}) {
  const [documents,setDocuments] = useState([])
  const [selectedFile,setSelectedFile] = useState(null)
  const [loading,setLoading] = useState(true)
  const [busy,setBusy] = useState(false)
  const [error,setError] = useState('')
  const [success,setSuccess] = useState('')

  const isApplicant = Number(loan.applicantId) === Number(user.id)
  const isSubmitter = Number(loan.appliedById) === Number(user.id)
  const isAdmin = user.role === 'ADMIN'
  const isCurrentApprover = ['DDO','HOO'].includes(user.role)
    && loan.status === 'PENDING_APPROVAL'
    && loan.currentApproverRole === user.role
  const canUpload = isApplicant || isSubmitter || isAdmin || isCurrentApprover

  async function loadDocuments(showLoading=true) {
    if (showLoading) setLoading(true)
    setError('')
    try {
      const response = await api.get(`/api/loans/${loan.loanId}/documents`)
      setDocuments(Array.isArray(response.data) ? response.data : [])
    } catch (e) {
      setDocuments([])
      setError(friendlyError(e))
    } finally {
      if (showLoading) setLoading(false)
    }
  }

  useEffect(() => {
    loadDocuments()
  }, [loan.loanId,user.id])

  async function uploadDocument(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    if (!selectedFile) {
      setError('Choose a PDF, JPG or PNG document first.')
      return
    }
    if (selectedFile.size > 10 * 1024 * 1024) {
      setError('The maximum document size is 10 MB.')
      return
    }

    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const body = new FormData()
      body.append('document', selectedFile)
      await api.post(`/api/loans/${loan.loanId}/documents`, body, { headers: { 'Content-Type': undefined } })
      setSelectedFile(null)
      const picker = formElement.querySelector('input[type="file"]')
      if (picker) picker.value = ''
      setSuccess('Document uploaded successfully.')
      await loadDocuments(false)
    } catch (e) {
      setError(friendlyError(e))
    } finally {
      setBusy(false)
    }
  }

  async function openDocument(file, asDownload=false) {
    setError('')
    try {
      const response = await api.get(
        `/api/loans/${loan.loanId}/documents/${file.documentId}/content`,
        { responseType: 'blob' }
      )
      const objectUrl = URL.createObjectURL(response.data)
      const link = window.document.createElement('a')
      link.href = objectUrl
      if (asDownload) {
        link.download = file.fileName
      } else {
        link.target = '_blank'
        link.rel = 'noopener noreferrer'
      }
      window.document.body.appendChild(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60000)
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  return <section className="panel" style={{marginTop:18}}>
    <div className="panel-heading">
      <div>
        <h3><Paperclip size={15} style={{verticalAlign:'-2px',marginRight:6}}/> Supporting documents</h3>
        <p>Private files attached to LN-{String(loan.loanId).padStart(5,'0')} · PDF, JPG and PNG · Maximum 10 MB each</p>
      </div>
      <span className="soft-tag">{documents.length}/10 files</span>
    </div>
    {error&&<Alert>{error}</Alert>}
    {success&&<Alert type="success">{success}</Alert>}
    {canUpload&&<form onSubmit={uploadDocument} style={{padding:'0 20px 16px',display:'flex',alignItems:'end',gap:10,flexWrap:'wrap'}}>
      <label className="field" style={{margin:0,flex:'1 1 260px'}}>
        <span>Choose supporting document</span>
        <input
          type="file"
          accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          onChange={event=>setSelectedFile(event.target.files?.[0]||null)}
        />
        <small>{selectedFile ? selectedFile.name+' · '+formatDocumentSize(selectedFile.size) : 'Documents are stored privately outside the public frontend folder.'}</small>
      </label>
      <button className="btn btn-primary" disabled={busy||!selectedFile||documents.length>=10}>
        {busy?<LoaderCircle className="spin" size={15}/>:<Paperclip size={15}/>} Upload document
      </button>
    </form>}
    {loading?<Spinner/>:documents.length===0
      ?<Empty title="No supporting documents" copy={canUpload?'Upload the documents required for this application.':'No documents have been attached to this application yet.'}/>
      :<div className="ticket-list" style={{padding:'0 20px 8px'}}>
        {documents.map(file=><div className="ticket-row" key={file.documentId}>
          <div className="ticket-symbol"><FileText size={18}/></div>
          <div className="ticket-main">
            <b>{file.fileName}</b>
            <small>{formatDocumentSize(file.fileSize)} · Uploaded by {file.uploadedBy} · {dateTime(file.uploadedAt)}</small>
          </div>
          <div style={{display:'flex',gap:6,flexWrap:'wrap'}}>
            <button type="button" className="btn btn-quiet btn-sm" onClick={()=>openDocument(file,false)}><Eye size={13}/> Preview</button>
            <button type="button" className="btn btn-quiet btn-sm" onClick={()=>openDocument(file,true)}><Download size={13}/> Download</button>
          </div>
        </div>)}
      </div>}
    <div className="form-note" style={{margin:'0 20px 12px'}}>
      <ShieldCheck size={16}/>
      <span>Only the applicant, application submitter, authorised current approver and administrators can access these files. File type is checked using its content signature.</span>
    </div>
  </section>
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
      <LoanDocuments loan={loan} user={user}/>
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


export function RecoveryHistory({user}) {
  const [records,setRecords]=useState([])
  const [scheduleRows,setScheduleRows]=useState([])
  const [ownLoans,setOwnLoans]=useState([])
  const [loading,setLoading]=useState(true)
  const [error,setError]=useState('')
  const [success,setSuccess]=useState('')
  const [query,setQuery]=useState('')
  const [loanIdQuery,setLoanIdQuery]=useState('')
  const [fromDate,setFromDate]=useState('')
  const [toDate,setToDate]=useState('')
  const [monthlyAmounts,setMonthlyAmounts]=useState({})
  const [savingLoanId,setSavingLoanId]=useState(null)

  async function load(){
    setLoading(true)
    setError('')
    try{
      const [recoveryResponse,scheduleResponse,loanResponse]=await Promise.all([
        api.get('/api/loans/recoveries',{params:{viewerId:user.id}}),
        api.get('/api/loans/recovery-schedules',{params:{viewerId:user.id}}),
        api.get('/api/loans/user/'+user.id)
      ])
      setRecords(Array.isArray(recoveryResponse.data)?recoveryResponse.data:[])
      setScheduleRows(Array.isArray(scheduleResponse.data)?scheduleResponse.data:[])
      setOwnLoans(Array.isArray(loanResponse.data)?loanResponse.data:[])
    }catch(e){
      setError(friendlyError(e))
      setRecords([])
      setScheduleRows([])
      setOwnLoans([])
    }finally{
      setLoading(false)
    }
  }

  useEffect(()=>{load()},[user.id])

  const totalRecovered=records.reduce((sum,item)=>sum+Number(item.recoveryAmount||0),0)
  const paidInstallments=scheduleRows.filter(item=>item.status==='PAID').length
  const pendingInstallments=scheduleRows.filter(item=>item.status!=='PAID').length
  const scheduledLoanIds=new Set(scheduleRows.map(item=>Number(item.loanId)))
  const recoveredByLoan=records.reduce((totals,item)=>{
    totals[item.loanId]=(totals[item.loanId]||0)+Number(item.recoveryAmount||0)
    return totals
  },{})
  const loansAwaitingSchedule=ownLoans.filter(loan=>
    loan.status==='APPROVED'
    &&!scheduledLoanIds.has(Number(loan.loanId))
    &&Number(loan.amount||0)-Number(recoveredByLoan[loan.loanId]||0)>0
  )

  const formatDate=value=>value
    ?new Date(String(value).slice(0,10)+'T00:00:00').toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})
    :'—'
  const firstDueDate=()=>{
    const now=new Date()
    return new Date(now.getFullYear(),now.getMonth()+1,1).toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})
  }

  async function createSchedule(loan){
    const monthlyAmount=Number(monthlyAmounts[loan.loanId]||0)
    const outstanding=Number(loan.amount||0)-Number(recoveredByLoan[loan.loanId]||0)
    if(!monthlyAmount||monthlyAmount<=0)return
    setSavingLoanId(loan.loanId)
    setError('')
    setSuccess('')
    try{
      await api.post('/api/loans/'+loan.loanId+'/recovery-schedule',{
        applicantId:user.id,
        monthlyAmount
      })
      setSuccess('Monthly recovery schedule created for LN-'+String(loan.loanId).padStart(5,'0')+'.')
      setMonthlyAmounts(current=>({...current,[loan.loanId]:''}))
      await load()
    }catch(e){
      setError(friendlyError(e))
    }finally{
      setSavingLoanId(null)
    }
  }

  // Accept IDs such as LN-00003, 00003 or 3; multiple IDs may be comma/space separated.
  const loanIdTokens=loanIdQuery.split(/[,;\s]+/)
    .map(value=>value.trim().toUpperCase().replace(/^LN[-_]?/,'').replace(/^0+(?=\d)/,''))
    .filter(Boolean)
  const matchesLoanId=loanId=>loanIdTokens.length===0||loanIdTokens.includes(String(Number(loanId)))

  const filteredSchedules=scheduleRows.filter(item=>{
    const dueDate=String(item.dueDate||'').slice(0,10)
    const searchText=[
      item.applicantName,item.applicantRole,item.applicantGroup,
      item.installmentNumber,item.status
    ].join(' ').toLowerCase()
    return matchesLoanId(item.loanId)
      &&searchText.includes(query.toLowerCase())
      &&(!fromDate||dueDate>=fromDate)
      &&(!toDate||dueDate<=toDate)
  })

  const filtered=records.filter(item=>{
    const receivedDate=String(item.receivedAt||'').slice(0,10)
    const searchText=[
      item.repaymentId,item.applicantName,item.applicantRole,
      item.applicantGroup,item.recordedByName,item.remarks
    ].join(' ').toLowerCase()
    return matchesLoanId(item.loanId)
      &&searchText.includes(query.toLowerCase())
      &&(!fromDate||receivedDate>=fromDate)
      &&(!toDate||receivedDate<=toDate)
  })

  // Keep each loan's scheduled instalments and actual payments together.
  const loanIds=Array.from(new Set([
    ...filteredSchedules.map(item=>Number(item.loanId)),
    ...filtered.map(item=>Number(item.loanId))
  ])).sort((a,b)=>a-b)

  const recoveryGroups=loanIds.map(loanId=>{
    const schedules=filteredSchedules.filter(item=>Number(item.loanId)===loanId)
    const payments=filtered.filter(item=>Number(item.loanId)===loanId)
    const sample=schedules[0]||payments[0]||{}
    return {
      loanId,
      applicantName:sample.applicantName,
      applicantRole:sample.applicantRole,
      applicantGroup:sample.applicantGroup,
      schedules,
      payments,
      scheduledTotal:schedules.reduce((sum,item)=>sum+Number(item.recoveryAmount||0),0),
      paidTotal:schedules.reduce((sum,item)=>sum+Number(item.paidAmount||0),0),
      paymentTotal:payments.reduce((sum,item)=>sum+Number(item.recoveryAmount||0),0)
    }
  })

  function clearFilters(){
    setQuery('')
    setLoanIdQuery('')
    setFromDate('')
    setToDate('')
  }

  return <>
    <PageHeader
      eyebrow="FINANCE OPERATIONS"
      title="Recovery history"
      subtitle={user.role==='EMPLOYEE'
        ?"Choose your monthly principal recovery amount and track every due date."
        :"Review scheduled instalments and principal recovery transactions across LoanDesk."}
      action={<button className="btn btn-quiet" onClick={load} disabled={loading}><RefreshCw size={16}/> Refresh</button>}
    />
    {error&&<Alert>{error}</Alert>}
    {success&&<Alert type="success">{success}</Alert>}

    <div className="stats-grid">
      <Stat label="Total recovered" value={loading?'—':money(totalRecovered)} detail="Principal already received" icon={Wallet} tone="green"/>
      <Stat label="Scheduled instalments" value={loading?'—':scheduleRows.length} detail="Across configured recovery plans" icon={CalendarDays} tone="blue"/>
      <Stat label="Paid instalments" value={loading?'—':paidInstallments} detail="Fully settled instalments" icon={CheckCircle2} tone="green"/>
      <Stat label="Pending instalments" value={loading?'—':pendingInstallments} detail="Unpaid or partly paid" icon={Clock3} tone="amber"/>
    </div>

    {user.role!=='ADMIN'&&user.role!=='SUPPORT'&&<section className="panel table-panel" style={{marginBottom:18}}>
      <div className="panel-heading">
        <div><h3>Choose your monthly recovery amount</h3><p>Available for your approved loans that do not already have a schedule.</p></div>
        <span className="soft-tag">0% interest</span>
      </div>
      <div className="form-note" style={{margin:'0 20px 14px'}}>
        <ShieldCheck size={17}/>
        <span>You choose the monthly principal deduction. LoanDesk calculates the number of months and creates a due date on the first day of each month, starting next month. No interest or extra charge is added; total scheduled recovery equals the outstanding principal. The final instalment may be smaller.</span>
      </div>
      {loading?<Spinner/>:loansAwaitingSchedule.length===0
        ?<div style={{padding:'0 20px 18px',fontSize:11,color:'#8994a7'}}>There are no approved loans awaiting a recovery schedule.</div>
        :<div className="ticket-list" style={{padding:'0 20px 10px'}}>
          {loansAwaitingSchedule.map(loan=>{
            const outstanding=Number(loan.amount||0)-Number(recoveredByLoan[loan.loanId]||0)
            const monthlyAmount=Number(monthlyAmounts[loan.loanId]||0)
            const months=monthlyAmount>0?Math.ceil(outstanding/monthlyAmount):null
            return <div className="ticket-row" key={loan.loanId} style={{alignItems:'center',flexWrap:'wrap'}}>
              <div className="ticket-symbol"><Wallet size={18}/></div>
              <div className="ticket-main" style={{minWidth:180}}>
                <b>{'LN-'+String(loan.loanId).padStart(5,'0')+' · '+money(outstanding)+' outstanding'}</b>
                <small>{loan.purpose||'Approved loan'} · Approved loan principal only</small>
              </div>
              <div style={{display:'flex',gap:8,alignItems:'end',flexWrap:'wrap',flex:1,justifyContent:'flex-end'}}>
                <label className="field" style={{margin:0,minWidth:180}}>
                  <span>Monthly recovery amount (₹)</span>
                  <input
                    type="number"
                    min="1"
                    max={outstanding}
                    step="0.01"
                    value={monthlyAmounts[loan.loanId]||''}
                    onChange={e=>setMonthlyAmounts(current=>({...current,[loan.loanId]:e.target.value}))}
                    placeholder="Enter amount"
                  />
                </label>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  disabled={savingLoanId===loan.loanId||!monthlyAmount||monthlyAmount<=0||monthlyAmount>outstanding}
                  onClick={()=>createSchedule(loan)}
                >{savingLoanId===loan.loanId?<LoaderCircle className="spin" size={14}/>:<Check size={14}/>} Set schedule</button>
              </div>
              {monthlyAmount>0&&monthlyAmount<=outstanding&&<div style={{flexBasis:'100%',fontSize:10,color:'#8390a4',paddingLeft:48}}>
                Estimated duration: <b>{months} month{months===1?'':'s'}</b> · First due date: <b>{firstDueDate()}</b> · No interest
              </div>}
            </div>
          })}
        </div>
      }
    </section>}

    <section className="panel table-panel">
      <div className="panel-heading">
        <div><h3>Recovery by loan number</h3><p>Each loan has its own instalment schedule and payment history.</p></div>
        <span className="soft-tag">{recoveryGroups.length} loans</span>
      </div>
      <div className="filter-bar" style={{paddingBottom:8}}>
        <div className="search-field" style={{maxWidth:'none',flex:'1 1 280px'}}>
          <Search size={16}/>
          <input aria-label="Search by loan IDs" placeholder="Search by loan ID(s): LN-00003, LN-00007" value={loanIdQuery} onChange={e=>setLoanIdQuery(e.target.value)}/>
        </div>
      </div>
      <div className="filter-bar">
        <div className="search-field"><Search size={16}/><input aria-label="Search borrower or recovery details" placeholder="Search borrower, status, officer or remarks…" value={query} onChange={e=>setQuery(e.target.value)}/></div>
        <label className="field" style={{margin:0,minWidth:140}}><span>From date</span><input type="date" value={fromDate} onChange={e=>setFromDate(e.target.value)}/></label>
        <label className="field" style={{margin:0,minWidth:140}}><span>To date</span><input type="date" min={fromDate||undefined} value={toDate} onChange={e=>setToDate(e.target.value)}/></label>
        <button className="btn btn-quiet btn-sm" onClick={clearFilters}>Clear filters</button>
      </div>
      {loading?<Spinner/>:recoveryGroups.length===0
        ?<Empty title="No recovery records found" copy={scheduleRows.length||records.length?'No loan records match your current filters.':'Once a monthly recovery schedule is created or a repayment is recorded, it will appear here under its loan number.'}/>
        :<div style={{display:'grid',gap:16,padding:'0 18px 18px'}}>
          {recoveryGroups.map(group=><section className="panel" key={group.loanId} style={{padding:0,overflow:'hidden',border:'1px solid #e8ecf3'}}>
            <div className="panel-heading" style={{padding:'14px 16px',margin:0,background:'#f8f9fc',borderBottom:'1px solid #edf0f5'}}>
              <div>
                <h3><Link className="id-link" to={'/loans/'+group.loanId}>{'LN-'+String(group.loanId).padStart(5,'0')}</Link></h3>
                <p>{group.applicantName||'Borrower'} · {titleCase(group.applicantRole)} · Group {group.applicantGroup||'—'}</p>
              </div>
              <div style={{textAlign:'right'}}>
                <b>{money(group.scheduledTotal||group.paymentTotal)}</b>
                <small style={{display:'block',color:'#8d99aa',marginTop:4}}>Scheduled / recorded in this view</small>
              </div>
            </div>

            <div style={{padding:'14px 16px 8px'}}>
              <div className="steps-heading" style={{marginBottom:8}}><span>SCHEDULED INSTALMENTS</span><span className="soft-tag">{group.schedules.length} instalments</span></div>
              {group.schedules.length===0
                ?<p style={{fontSize:10,color:'#98a1af',padding:'4px 0 10px'}}>No scheduled instalments for this loan yet.</p>
                :<div className="table-scroll">
                  <table>
                    <thead><tr>
                      <th>INSTALMENT</th>
                      <th>RECOVERY AMOUNT</th>
                      <th>DUE DATE</th>
                      <th>PAID AMOUNT</th>
                      <th>PAID ON</th>
                      <th>STATUS</th>
                    </tr></thead>
                    <tbody>
                      {group.schedules.map(item=><tr key={item.installmentId}>
                        <td>{'#'+item.installmentNumber}</td>
                        <td className="amount-cell">{money(item.recoveryAmount)}</td>
                        <td>{formatDate(item.dueDate)}</td>
                        <td>{money(item.paidAmount)}</td>
                        <td>{item.paidAt?dateTime(item.paidAt):'—'}</td>
                        <td><span className={'status-pill '+(item.status==='PAID'?'success':item.status==='PARTIALLY_PAID'?'warning':'neutral')}><i/>{titleCase(item.status)}</span></td>
                      </tr>)}
                    </tbody>
                  </table>
                </div>
              }
              {group.schedules.length>0&&<div style={{display:'flex',justifyContent:'flex-end',gap:18,flexWrap:'wrap',fontSize:10,color:'#8190a5',padding:'9px 0 4px'}}>
                <span>Scheduled: <b>{money(group.scheduledTotal)}</b></span>
                <span>Paid against schedule: <b>{money(group.paidTotal)}</b></span>
                <span>Balance: <b>{money(Math.max(0,group.scheduledTotal-group.paidTotal))}</b></span>
              </div>}
            </div>

            <div style={{padding:'10px 16px 16px'}}>
              <div className="steps-heading" style={{marginBottom:8}}><span>ACTUAL RECOVERY PAYMENTS</span><span className="soft-tag">{group.payments.length} payments</span></div>
              {group.payments.length===0
                ?<p style={{fontSize:10,color:'#98a1af',padding:'4px 0 2px'}}>No payments have been recorded against this loan yet.</p>
                :<div className="table-scroll">
                  <table>
                    <thead><tr>
                      <th>RECEIVED ON</th>
                      <th>RECOVERY REF</th>
                      <th>RECOVERY AMOUNT</th>
                      <th>RECORDED BY</th>
                      <th>REMARKS</th>
                    </tr></thead>
                    <tbody>
                      {group.payments.map(item=><tr key={item.repaymentId}>
                        <td>{dateTime(item.receivedAt)}</td>
                        <td className="id-link">{'REC-'+String(item.repaymentId).padStart(5,'0')}</td>
                        <td className="amount-cell">{money(item.recoveryAmount)}</td>
                        <td>{item.recordedByName||'—'}</td>
                        <td className="purpose-cell">{item.remarks||'—'}</td>
                      </tr>)}
                    </tbody>
                  </table>
                </div>
              }
            </div>
          </section>)}
        </div>
      }
    </section>

  </>
}

export function Support({user}) {
  const [tickets,setTickets]=useState([])
  const [selectedTicket,setSelectedTicket]=useState(null)
  const [loading,setLoading]=useState(true)
  const [busy,setBusy]=useState(false)
  const [error,setError]=useState('')
  const [success,setSuccess]=useState('')
  const [showForm,setShowForm]=useState(false)
  const [query,setQuery]=useState('')
  const [statusFilter,setStatusFilter]=useState('ALL')
  const [reply,setReply]=useState('')
  const [form,setForm]=useState({
    category:'OTHER',
    subject:'',
    description:'',
    priority:'MEDIUM',
    loanId:''
  })

  async function load(showSpinner=true) {
    if(showSpinner)setLoading(true)
    try {
      const response=await api.get('/api/support/tickets/my')
      const rows=Array.isArray(response.data)?response.data:[]
      setTickets(rows)
      setSelectedTicket(current=>current
        ? rows.find(ticket=>ticket.id===current.id)||current
        : null)
      setError('')
    } catch(e) {
      setError(friendlyError(e))
    } finally {
      if(showSpinner)setLoading(false)
    }
  }

  useEffect(()=>{
    load()
    const refreshId=setInterval(()=>load(false),15000)
    return ()=>clearInterval(refreshId)
  },[user.id])

  async function openTicket(ticketId) {
    setError('')
    setSuccess('')
    try {
      const response=await api.get(`/api/support/tickets/${ticketId}`)
      setSelectedTicket(response.data)
    } catch(e) {
      setError(friendlyError(e))
    }
  }

  async function create(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const body={
        ...form,
        loanId:form.loanId?Number(form.loanId):null
      }
      const response=await api.post('/api/support/tickets',body)
      const created=response.data
      setTickets(current=>[created,...current.filter(ticket=>ticket.id!==created.id)])
      setSelectedTicket(created)
      setShowForm(false)
      setForm({category:'OTHER',subject:'',description:'',priority:'MEDIUM',loanId:''})
      setSuccess(`Ticket ${created.ticketNumber} submitted. You can track its status and updates below.`)
      await load(false)
      setSelectedTicket(created)
    } catch(e) {
      setError(friendlyError(e))
    } finally {
      setBusy(false)
    }
  }

  async function closeTicket(ticketId) {
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      await api.patch(`/api/support/tickets/${ticketId}/status`,{status:'CLOSED'})
      setSuccess('Ticket closed successfully.')
      await load(false)
      await openTicket(ticketId)
    } catch(e) {
      setError(friendlyError(e))
    } finally {
      setBusy(false)
    }
  }

  async function addReply(e) {
    e.preventDefault()
    if(!selectedTicket||!reply.trim())return
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      await api.post(`/api/support/tickets/${selectedTicket.id}/comments`,{
        body:reply.trim(),
        internalNote:false
      })
      setReply('')
      await load(false)
      await openTicket(selectedTicket.id)
      setSuccess('Your reply was added to the ticket.')
    } catch(e) {
      setError(friendlyError(e))
    } finally {
      setBusy(false)
    }
  }

  const count=status=>tickets.filter(ticket=>ticket.status===status).length
  const resolvedCount=count('RESOLVED')+count('CLOSED')
  const filtered=tickets.filter(ticket=>{
    const matchesStatus=statusFilter==='ALL'||ticket.status===statusFilter
    const searchText=[
      ticket.ticketNumber,ticket.subject,ticket.category,ticket.priority,
      ticket.status,ticket.assigneeName,ticket.loanId
    ].join(' ').toLowerCase()
    return matchesStatus&&searchText.includes(query.toLowerCase())
  })
  const stages=['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED']
  const currentStage=selectedTicket?stages.indexOf(selectedTicket.status):-1

  function eventTitle(item) {
    if(item.eventType==='CREATED')return 'Ticket submitted'
    if(item.eventType==='ASSIGNED')return 'Assigned to support'
    if(item.eventType==='REASSIGNED')return 'Support officer changed'
    if(item.eventType==='STATUS_CHANGED')return 'Status updated'
    return titleCase(item.eventType)
  }

  return <>
    <PageHeader
      eyebrow="HELP & SUPPORT"
      title="Support centre"
      subtitle="Raise requests and track every ticket from submission through resolution."
      action={<button className="btn btn-primary" onClick={()=>setShowForm(value=>!value)}><Plus size={16}/> Raise a ticket</button>}
    />
    {error&&<Alert>{error}</Alert>}
    {success&&<Alert type="success">{success}</Alert>}

    <div className="stats-grid">
      <Stat label="All my tickets" value={loading?'—':tickets.length} detail="Requests submitted" icon={MessageSquare}/>
      <Stat label="Open / pending" value={loading?'—':count('OPEN')} detail="Awaiting assignment" icon={Clock3} tone="amber"/>
      <Stat label="Being handled" value={loading?'—':count('ASSIGNED')+count('IN_PROGRESS')} detail="Assigned or in progress" icon={Activity} tone="blue"/>
      <Stat label="Resolved / closed" value={loading?'—':resolvedCount} detail="Completed requests" icon={CheckCircle2} tone="green"/>
    </div>

    {showForm&&<form className="panel ticket-form" onSubmit={create}>
      <div className="panel-heading">
        <div><h3>New support request</h3><p>After submission, your ticket will appear below with a tracking number and live status.</p></div>
        <button type="button" className="icon-btn" onClick={()=>setShowForm(false)}><X size={17}/></button>
      </div>
      <div className="form-grid">
        <label className="field"><span>Category</span><select value={form.category} onChange={e=>setForm({...form,category:e.target.value})}>{['LOGIN_ACCESS','EMPLOYEE_DETAILS','LOAN_SUBMISSION','ELIGIBILITY','DOCUMENT_UPLOAD','APPLICATION_STATUS','OTHER'].map(value=><option key={value} value={value}>{titleCase(value)}</option>)}</select></label>
        <label className="field"><span>Priority</span><select value={form.priority} onChange={e=>setForm({...form,priority:e.target.value})}>{['LOW','MEDIUM','HIGH','URGENT'].map(value=><option key={value}>{value}</option>)}</select></label>
        <label className="field full"><span>Subject</span><input required maxLength="180" value={form.subject} onChange={e=>setForm({...form,subject:e.target.value})} placeholder="What do you need help with?"/></label>
        <label className="field"><span>Loan ID (optional)</span><input type="number" min="1" value={form.loanId} onChange={e=>setForm({...form,loanId:e.target.value})} placeholder="e.g. 12"/></label>
        <label className="field full"><span>Description</span><textarea required rows="3" maxLength="5000" value={form.description} onChange={e=>setForm({...form,description:e.target.value})} placeholder="Describe the issue in a little more detail…"/></label>
      </div>
      {['EMPLOYEE','DDO'].includes(user.role)&&<LoanDeskAssistant mode="ticket" user={user} onApply={values=>setForm(current=>({...current,...values}))}/>}
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}><Send size={15}/> {busy?'Submitting…':'Submit ticket'}</button></div>
    </form>}

    <div className="detail-grid">
      <section className="panel table-panel">
        <div className="panel-heading">
          <div><h3>My support tickets</h3><p>Every ticket you have submitted, including resolved and closed requests</p></div>
          <button className="btn btn-quiet btn-sm" onClick={()=>load()} disabled={loading}><RefreshCw size={15}/> Refresh</button>
        </div>
        <div className="filter-bar" style={{padding:'0 0 14px',flexWrap:'wrap'}}>
          <div className="search-field" style={{maxWidth:'none',flexBasis:'100%'}}><Search size={15}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search ticket number, subject or linked loan…"/></div>
          <select className="select-control" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}>
            {['ALL','OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'].map(value=><option key={value} value={value}>{value==='ALL'?'All statuses':value==='OPEN'?'Open / pending':titleCase(value)}</option>)}
          </select>
        </div>
        {loading?<Spinner/>:filtered.length===0
          ?<Empty title={tickets.length?'No matching tickets':'No support tickets yet'} copy={tickets.length?'Try another status or search term.':'When you raise a ticket, its tracking details and status will appear here.'}/>
          :<div className="ticket-list" style={{padding:0}}>
            {filtered.map(ticket=><button
              key={ticket.id}
              type="button"
              onClick={()=>openTicket(ticket.id)}
              className="ticket-row"
              style={{
                display:'flex',width:'100%',textAlign:'left',background:selectedTicket?.id===ticket.id?'#f3f5ff':'transparent',
                border:0,borderTop:'1px solid #eff1f5',cursor:'pointer',padding:'13px 8px',borderRadius:7
              }}
            >
              <div className="ticket-symbol"><MessageSquare size={18}/></div>
              <div className="ticket-main">
                <b>{ticket.subject}</b>
                <small>{ticket.ticketNumber} · {titleCase(ticket.category)} · {dateTime(ticket.createdAt)}</small>
                <small>{ticket.assigneeName? `Assigned to ${ticket.assigneeName}`:'Awaiting assignment'} · Updated {dateTime(ticket.updatedAt)}</small>
              </div>
              <Status value={ticket.status}/>
              <ChevronRight size={16}/>
            </button>)}
          </div>
        }
      </section>

      <section className="panel detail-main">
        {!selectedTicket
          ?<Empty title="Select a ticket to track" copy="Choose one of your requests to see its current status, assigned officer, replies, resolution and full activity timeline."/>
          :<>
            <div className="panel-heading">
              <div>
                <div className="eyebrow">{selectedTicket.ticketNumber}</div>
                <h3>{selectedTicket.subject}</h3>
                <p>Submitted {dateTime(selectedTicket.createdAt)} · Last updated {dateTime(selectedTicket.updatedAt)}</p>
              </div>
              <Status value={selectedTicket.status}/>
            </div>

            <div style={{display:'grid',gridTemplateColumns:'repeat(2,minmax(0,1fr))',gap:12,margin:'16px 0'}}>
              <div><small style={{display:'block',color:'#98a1af'}}>CATEGORY</small><b>{titleCase(selectedTicket.category)}</b></div>
              <div><small style={{display:'block',color:'#98a1af'}}>PRIORITY</small><b>{titleCase(selectedTicket.priority)}</b></div>
              <div><small style={{display:'block',color:'#98a1af'}}>ASSIGNED OFFICER</small><b>{selectedTicket.assigneeName||'Not assigned yet'}</b></div>
              <div><small style={{display:'block',color:'#98a1af'}}>LINKED LOAN</small><b>{selectedTicket.loanId?`LN-${String(selectedTicket.loanId).padStart(5,'0')}`:'Not linked'}</b></div>
            </div>

            <div className="detail-purpose">
              <small>REQUEST DESCRIPTION</small>
              <p style={{whiteSpace:'pre-wrap'}}>{selectedTicket.description}</p>
            </div>

            <div className="steps-editor">
              <div className="steps-heading"><span>REQUEST PROGRESS</span></div>
              <div style={{display:'grid',gridTemplateColumns:'repeat(5,minmax(0,1fr))',gap:4,margin:'12px 0 8px'}}>
                {stages.map((stage,index)=>{
                  const completed=currentStage>=index
                  return <div key={stage} style={{textAlign:'center',minWidth:0}}>
                    <div style={{height:5,borderRadius:5,background:completed?'#16a477':'#e8edf4',marginBottom:7}}/>
                    <small style={{fontSize:8,color:completed?'#267b61':'#98a1af',fontWeight:completed?700:400}}>{stage==='OPEN'?'OPEN':stage==='IN_PROGRESS'?'IN PROGRESS':stage}</small>
                  </div>
                })}
              </div>
              <p style={{fontSize:10,color:'#748198',margin:'8px 0 0'}}>Current state: <b>{selectedTicket.status==='OPEN'?'Open · Pending assignment':titleCase(selectedTicket.status)}</b></p>
            </div>

            {selectedTicket.resolution&&<div className="form-note" style={{marginTop:15}}>
              <CheckCircle2 size={18}/>
              <span><b>Resolution summary</b><br/>{selectedTicket.resolution}<br/><small>Resolved {dateTime(selectedTicket.resolvedAt)}</small></span>
            </div>}

            <div className="steps-editor">
              <div className="steps-heading"><span>STATUS & ASSIGNMENT HISTORY</span></div>
              {selectedTicket.activities?.length
                ?<div className="timeline">{selectedTicket.activities.map(item=><div className="timeline-item" key={item.id}>
                  <div className="timeline-dot"><Check size={12}/></div>
                  <div>
                    <b>{eventTitle(item)}</b>
                    <small>{item.actorName} · {dateTime(item.createdAt)}</small>
                    {item.fromStatus&&<small>{titleCase(item.fromStatus)} → {titleCase(item.toStatus)}</small>}
                    {item.message&&<p>{item.message}</p>}
                  </div>
                </div>)}</div>
                :<p style={{fontSize:10,color:'#98a1af'}}>No status history is available for this ticket yet. Refresh to load the latest state.</p>
              }
            </div>

            <div className="steps-editor">
              <div className="steps-heading"><span>REPLIES FROM SUPPORT</span></div>
              {selectedTicket.comments?.length
                ?<div className="timeline">{selectedTicket.comments.map(item=><div className="timeline-item" key={item.id}>
                  <div className="timeline-dot"><MessageSquare size={12}/></div>
                  <div><b>{item.authorName}</b><small>{dateTime(item.createdAt)}</small><p style={{whiteSpace:'pre-wrap'}}>{item.body}</p></div>
                </div>)}</div>
                :<p style={{fontSize:10,color:'#98a1af'}}>No replies have been added yet.</p>
              }
              {selectedTicket.status!=='CLOSED'&&<form onSubmit={addReply} className="ticket-form" style={{padding:'10px 0 0'}}>
                <label className="field"><span>Send a reply</span><textarea rows="3" value={reply} onChange={e=>setReply(e.target.value)} placeholder="Add more information or reply to the support team…"/></label>
                <div className="form-actions" style={{padding:0}}><button className="btn btn-primary" disabled={busy||!reply.trim()}><Send size={14}/> Send reply</button></div>
              </form>}
            </div>

            {selectedTicket.status==='RESOLVED'&&<div className="form-actions" style={{justifyContent:'flex-start'}}>
              <button className="btn btn-primary" disabled={busy} onClick={()=>closeTicket(selectedTicket.id)}><CheckCircle2 size={15}/> Confirm and close ticket</button>
            </div>}
            {selectedTicket.status==='CLOSED'&&<Alert type="success">This ticket is closed. Its details and full history remain available here.</Alert>}
          </>
        }
      </section>
    </div>
  </>
}

export function SupportDesk({user}) {
  const [tickets,setTickets]=useState([])
  const [supportUsers,setSupportUsers]=useState([])
  const [selected,setSelected]=useState(null)
  const [loading,setLoading]=useState(true)
  const [busy,setBusy]=useState(false)
  const [error,setError]=useState('')
  const [success,setSuccess]=useState('')
  const [query,setQuery]=useState('')
  const [statusFilter,setStatusFilter]=useState('ALL')
  const [assigneeId,setAssigneeId]=useState('')
  const [comment,setComment]=useState('')
  const [internalNote,setInternalNote]=useState(false)
  const [resolution,setResolution]=useState('')

  const isAdmin=user.role==='ADMIN'

  async function loadQueue(){
    setLoading(true)
    setError('')
    try{
      const response=await api.get('/api/support/tickets/queue')
      const rows=Array.isArray(response.data)?response.data:[]
      setTickets(rows)
      if(selected?.id&&!rows.some(ticket=>ticket.id===selected.id)){
        setSelected(null)
      }
    }catch(e){
      setError(friendlyError(e))
      setTickets([])
    }finally{
      setLoading(false)
    }
  }

  async function loadSupportUsers(){
    if(!isAdmin)return
    try{
      const response=await api.get('/api/demo/users')
      setSupportUsers((Array.isArray(response.data)?response.data:[]).filter(item=>item.role==='SUPPORT'))
    }catch(e){
      setError(friendlyError(e))
    }
  }

  async function openTicket(ticketId){
    setError('')
    setSuccess('')
    try{
      const response=await api.get(`/api/support/tickets/${ticketId}`)
      setSelected(response.data)
      setAssigneeId(response.data.assigneeId?String(response.data.assigneeId):'')
      setResolution(response.data.resolution||'')
    }catch(e){
      setError(friendlyError(e))
    }
  }

  useEffect(()=>{
    loadQueue()
    loadSupportUsers()
  },[user.id,user.role])

  async function refresh(){
    await loadQueue()
    if(selected?.id)await openTicket(selected.id)
  }

  async function assignTicket(e){
    e.preventDefault()
    if(!selected||!assigneeId)return
    setBusy(true);setError('');setSuccess('')
    try{
      await api.patch(`/api/support/tickets/${selected.id}/assign`,{assigneeId:Number(assigneeId)})
      setSuccess('Ticket assigned successfully.')
      await loadQueue()
      await openTicket(selected.id)
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  async function changeStatus(nextStatus){
    if(!selected)return
    if(nextStatus==='RESOLVED'&&!resolution.trim()){
      setError('Enter a resolution summary before resolving this ticket.')
      return
    }
    setBusy(true);setError('');setSuccess('')
    try{
      await api.patch(`/api/support/tickets/${selected.id}/status`,{
        status:nextStatus,
        resolution:nextStatus==='RESOLVED'?resolution:null
      })
      setSuccess(`Ticket moved to ${titleCase(nextStatus)}.`)
      await loadQueue()
      await openTicket(selected.id)
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  async function addComment(e){
    e.preventDefault()
    if(!selected||!comment.trim())return
    setBusy(true);setError('');setSuccess('')
    try{
      await api.post(`/api/support/tickets/${selected.id}/comments`,{
        body:comment.trim(),
        internalNote
      })
      setComment('')
      setInternalNote(false)
      await openTicket(selected.id)
      setSuccess(internalNote?'Internal note added.':'Reply added.')
    }catch(e){setError(friendlyError(e))}
    finally{setBusy(false)}
  }

  const openCount=tickets.filter(t=>t.status==='OPEN').length
  const assignedCount=tickets.filter(t=>t.status==='ASSIGNED').length
  const activeCount=tickets.filter(t=>t.status==='IN_PROGRESS').length
  const resolvedCount=tickets.filter(t=>['RESOLVED','CLOSED'].includes(t.status)).length
  const filtered=tickets.filter(t=>{
    const matchesStatus=statusFilter==='ALL'||t.status===statusFilter
    const haystack=[t.ticketNumber,t.subject,t.requesterName,t.category,t.priority,t.status,t.assigneeName].join(' ').toLowerCase()
    return matchesStatus&&haystack.includes(query.toLowerCase())
  })

  return <>
    <PageHeader
      eyebrow="SERVICE OPERATIONS"
      title="Support staff desk"
      subtitle={isAdmin
        ?"Review the full ticket queue and assign each request to a support officer."
        :"Work your assigned tickets, pick up unassigned requests and record resolutions."}
      action={<button className="btn btn-quiet" onClick={refresh} disabled={loading||busy}><RefreshCw size={15}/> Refresh queue</button>}
    />
    {error&&<Alert>{error}</Alert>}
    {success&&<Alert type="success">{success}</Alert>}
    <div className="stats-grid">
      <Stat label={isAdmin?'All tickets':'Visible queue'} value={loading?'—':tickets.length} detail="Tickets in this queue" icon={Inbox}/>
      <Stat label="Unassigned / open" value={loading?'—':openCount} detail="Awaiting an owner" icon={Clock3} tone="amber"/>
      <Stat label="Assigned" value={loading?'—':assignedCount+activeCount} detail="Assigned or being handled" icon={UserRound} tone="blue"/>
      <Stat label="Resolved / closed" value={loading?'—':resolvedCount} detail="Completed requests" icon={CheckCircle2} tone="green"/>
    </div>
    <div className="detail-grid">
      <section className="panel table-panel">
        <div className="panel-heading">
          <div><h3>{isAdmin?'All support tickets':'Your work queue'}</h3><p>{isAdmin?'All incoming requests across LoanDesk':'Unassigned tickets and tickets assigned to you'}</p></div>
          <span className="soft-tag">{filtered.length} tickets</span>
        </div>
        <div className="filter-bar" style={{padding:'0 0 14px',flexWrap:'wrap'}}>
          <div className="search-field" style={{maxWidth:'none',flexBasis:'100%'}}><Search size={15}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search ticket, subject, requester or assignee…"/></div>
          <select className="select-control" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}>
            {['ALL','OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'].map(value=><option key={value} value={value}>{value==='ALL'?'All statuses':titleCase(value)}</option>)}
          </select>
        </div>
        {loading?<Spinner/>:filtered.length===0
          ?<Empty title={tickets.length?'No matching tickets':'No tickets in this queue'} copy={tickets.length?'Change your search or status filter.':'New support requests will appear here when submitted.'}/>
          :<div className="ticket-list" style={{padding:0}}>
            {filtered.map(ticket=><button
              type="button"
              key={ticket.id}
              onClick={()=>openTicket(ticket.id)}
              className="ticket-row"
              style={{
                width:'100%',textAlign:'left',background:selected?.id===ticket.id?'#f5f7ff':'transparent',
                border:0,borderTop:'1px solid #eff1f5',cursor:'pointer',padding:'13px 8px',borderRadius:7
              }}
            >
              <div className="ticket-symbol"><MessageSquare size={17}/></div>
              <div className="ticket-main">
                <b>{ticket.subject}</b>
                <small>{ticket.ticketNumber} · {ticket.requesterName} · {titleCase(ticket.priority)}</small>
                <small>{ticket.assigneeName? `Assigned to ${ticket.assigneeName}`:'Unassigned'} · {dateTime(ticket.createdAt)}</small>
              </div>
              <Status value={ticket.status}/>
              <ChevronRight size={16}/>
            </button>)}
          </div>
        }
      </section>

      <section className="panel detail-main">
        {!selected
          ?<Empty title="Select a ticket" copy="Choose a request from the queue to review its details and take action."/>
          :<>
            <div className="panel-heading">
              <div><div className="eyebrow">{selected.ticketNumber}</div><h3>{selected.subject}</h3><p>{titleCase(selected.category)} · Submitted {dateTime(selected.createdAt)}</p></div>
              <Status value={selected.status}/>
            </div>
            <div className="detail-kv">
              <div><small>Requester</small><b>{selected.requesterName}</b></div>
              <div><small>Priority</small><b>{titleCase(selected.priority)}</b></div>
              <div><small>Linked loan</small><b>{selected.loanId? `LN-${String(selected.loanId).padStart(5,'0')}`:'Not linked'}</b></div>
              <div><small>Assigned to</small><b>{selected.assigneeName||'Unassigned'}</b></div>
            </div>
            <div className="detail-purpose"><small>REQUEST DESCRIPTION</small><p style={{whiteSpace:'pre-wrap'}}>{selected.description}</p></div>

            {isAdmin&&selected.status!=='CLOSED'&&<form onSubmit={assignTicket} className="steps-editor">
              <div className="steps-heading"><span>ASSIGN SUPPORT OFFICER</span></div>
              <div style={{display:'flex',gap:8,alignItems:'end',flexWrap:'wrap'}}>
                <label className="field" style={{flex:1,margin:0}}><span>Support officer</span>
                  <select required value={assigneeId} onChange={e=>setAssigneeId(e.target.value)}>
                    <option value="">Choose an officer</option>
                    {supportUsers.map(person=><option key={person.id} value={person.id}>{person.name} ({person.username})</option>)}
                  </select>
                </label>
                <button className="btn btn-primary" disabled={busy||!assigneeId}>Assign ticket</button>
              </div>
            </form>}

            {(user.role==='SUPPORT'||isAdmin)&&selected.status!=='CLOSED'&&<div className="steps-editor">
              <div className="steps-heading"><span>WORKFLOW ACTIONS</span></div>
              {user.role==='SUPPORT'&&['OPEN','ASSIGNED'].includes(selected.status)&&<button className="btn btn-primary" disabled={busy} onClick={()=>changeStatus('IN_PROGRESS')}>Start working <ArrowRight size={15}/></button>}
              {selected.status==='IN_PROGRESS'&&<>
                <label className="field"><span>Resolution summary</span><textarea rows="3" value={resolution} onChange={e=>setResolution(e.target.value)} placeholder="Explain how the issue was resolved…"/></label>
                <button className="btn btn-primary" disabled={busy||!resolution.trim()} onClick={()=>changeStatus('RESOLVED')}>Mark resolved <CheckCircle2 size={15}/></button>
              </>}
              {selected.status==='RESOLVED'&&<button className="btn btn-quiet" disabled={busy} onClick={()=>changeStatus('CLOSED')}>Close ticket <Check size={15}/></button>}
            </div>}

            <div className="steps-editor">
              <div className="steps-heading"><span>CONVERSATION & NOTES</span></div>
              {selected.comments?.length
                ?<div className="timeline">{selected.comments.map(item=><div className="timeline-item" key={item.id}>
                  <div className="timeline-dot"><MessageSquare size={12}/></div>
                  <div><b>{item.authorName} {item.internalNote&&<span className="soft-tag">Internal note</span>}</b><small>{dateTime(item.createdAt)}</small><p style={{whiteSpace:'pre-wrap'}}>{item.body}</p></div>
                </div>)}</div>
                :<p style={{fontSize:10,color:'#98a1af'}}>No comments have been added yet.</p>
              }
              <form onSubmit={addComment} className="ticket-form" style={{padding:'10px 0 0'}}>
                <label className="field"><span>Add a reply or note</span><textarea required rows="3" value={comment} onChange={e=>setComment(e.target.value)} placeholder="Write an update for the requester…"/></label>
                <label style={{display:'flex',alignItems:'center',gap:8,fontSize:10,color:'#66748a',marginBottom:12}}>
                  <input type="checkbox" checked={internalNote} onChange={e=>setInternalNote(e.target.checked)}/> Internal note (visible only to support staff)
                </label>
                <div className="form-actions" style={{padding:0}}><button className="btn btn-primary" disabled={busy||!comment.trim()}><Send size={14}/> Add comment</button></div>
              </form>
            </div>
          </>
        }
      </section>
    </div>
  </>
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