(()=>{
function viewerTools(){
 if(window.__SMV){window.__SMV.show();return}
 const D=document,S=Math.max(1,Math.min(3,innerWidth/((screen&&screen.width)||innerWidth)));
 let tile=null;
 D.addEventListener('pointerdown',e=>{const t=e.target.closest&&e.target.closest('.TileView_IV');if(t)tile=t},true);
 const vis=t=>t&&t.isConnected&&t.offsetWidth>0;
 const getTile=()=>{if(vis(tile))return tile;const a=D.activeElement&&D.activeElement.closest&&D.activeElement.closest('.TileView_IV');if(vis(a))return a;return [...D.querySelectorAll('.TileView_IV')].find(vis)};
 const key=(code,k,kc)=>{const t=getTile();if(!t)return;try{t.focus({preventScroll:true})}catch(e){}for(const ty of['keydown','keyup']){const e=new KeyboardEvent(ty,{key:k,code,location:code.indexOf('Numpad')==0?3:0,bubbles:true,cancelable:true});Object.defineProperty(e,'keyCode',{get:()=>kc});Object.defineProperty(e,'which',{get:()=>kc});t.dispatchEvent(e)}};
 const dbl=()=>{const t=getTile();if(!t)return;const r=t.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2,el=D.elementFromPoint(x,y)||t,o={bubbles:true,cancelable:true,clientX:x,clientY:y,view:window,button:0};for(const ty of['mousedown','mouseup','click','mousedown','mouseup','click'])el.dispatchEvent(new MouseEvent(ty,o));el.dispatchEvent(new MouseEvent('dblclick',Object.assign({detail:2},o)));tile=null};
 const layout=async id=>{tile=null;let b=D.getElementById(id);
  if(!b){const lb=[...D.querySelectorAll('[title]')].find(e=>/distribuci[oó]n de p[aá]gina/i.test(e.title));if(!lb)return;lb.click();for(let i=0;i<30&&!(b=D.getElementById(id));i++)await new Promise(r=>setTimeout(r,100))}
  if(!b)return;b.click();await new Promise(r=>setTimeout(r,300));
  const m=D.querySelector('.modal.in');if(m){const x=m.querySelector('[data-dismiss]');if(x)x.click()}};
 const dropSeries=cell=>{const t=getTile(),src=cell.querySelector('[draggable=true]');if(!t||!src)return;
  const cv=[...t.querySelectorAll('canvas')].sort((a,b)=>b.offsetWidth*b.offsetHeight-a.offsetWidth*a.offsetHeight)[0]||t;
  const r=t.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2,sr=src.getBoundingClientRect(),dt=new DataTransfer();
  const ev=(el,ty,cx,cy)=>el.dispatchEvent(new DragEvent(ty,{bubbles:true,cancelable:true,view:window,clientX:cx,clientY:cy,dataTransfer:dt}));
  ev(src,'dragstart',sr.left+sr.width/2,sr.top+sr.height/2);setTimeout(()=>{ev(cv,'dragenter',x,y);ev(cv,'dragover',x,y);setTimeout(()=>{ev(cv,'drop',x,y);ev(src,'dragend',x,y)},40)},40)};
 const panel=D.createElement('div');panel.id='smvser';
 const openSeries=()=>{if(panel.style.display=='flex'){panel.style.display='none';return}
  const rows=[...D.querySelectorAll('.SP_StudyRow_L')].filter(r=>r.offsetParent);panel.replaceChildren();
  rows.forEach((row,ri)=>{const cells=[...row.querySelectorAll('.SP_ThumbnailCell_L')].filter(c=>c.querySelector('[draggable=true]')&&c.querySelector('.SP_DescriptionText'));if(!cells.length)return;
   if(rows.length>1){const h=D.createElement('div');h.className='h';h.textContent=ri==0?'Estudio actual':'Estudio previo '+ri;panel.append(h)}
   cells.forEach(c=>{const d=c.querySelector('.SP_DescriptionText').textContent.trim();if(/^Marcadores$/i.test(d))return;const b=D.createElement('button');b.textContent=d;b.addEventListener('click',e=>{e.preventDefault();e.stopPropagation();panel.style.display='none';dropSeries(c)});panel.append(b)})});
  if(!panel.children.length){const h=D.createElement('div');h.className='h';h.textContent='No encontré series';panel.append(h)}
  panel.style.bottom=(bar.offsetHeight+8)+'px';panel.style.display='flex'};
 const closeZoomDlg=()=>{for(const b of D.querySelectorAll('button,input[type=button],span,div')){if(b.children.length||!b.offsetParent||(b.textContent||b.value||'').trim()!='Cerrar')continue;let p=b.parentElement;for(let i=0;i<6&&p&&!/zoom/i.test(p.textContent);i++)p=p.parentElement;if(p&&/zoom/i.test(p.textContent))b.click()}};
 let n=0;const iv=setInterval(()=>{closeZoomDlg();if(++n>40)clearInterval(iv)},800);
 const px=v=>Math.round(v*S)+'px';
 const st=D.createElement('style');st.textContent=`#smv{position:fixed;left:0;right:0;bottom:0;z-index:2147483647;background:rgba(13,26,25,.95);border-top:2px solid #76D7B5;padding:${px(6)};display:flex;flex-direction:column;gap:${px(6)};font-family:Roboto,Arial,sans-serif;touch-action:manipulation;user-select:none;-webkit-user-select:none}
#smv .r{display:flex;gap:${px(6)}}
#smv button{flex:1;font-size:${px(15)};font-weight:700;padding:${px(11)} 0;border-radius:${px(9)};border:1px solid #2E4140;background:#1B2929;color:#E6EEEC;touch-action:manipulation}
#smv button.p{color:#76D7B5;border-color:#76D7B5}
#smv button:active{background:#38946D;color:#FFFFFF}
#smvser{position:fixed;left:${px(6)};right:${px(6)};bottom:${px(196)};max-height:55vh;overflow-y:auto;z-index:2147483647;background:rgba(13,26,25,.97);border:2px solid #76D7B5;border-radius:${px(12)};padding:${px(8)};display:none;flex-direction:column;gap:${px(6)};font-family:Roboto,Arial,sans-serif}
#smvser .h{color:#9AB0AB;font-size:${px(13)};padding:${px(4)} ${px(2)} 0}
#smvser button{font-size:${px(15)};font-weight:600;text-align:left;padding:${px(12)};border-radius:${px(9)};border:1px solid #2E4140;background:#1B2929;color:#E6EEEC}
#smvpill{position:fixed;right:${px(8)};bottom:${px(8)};z-index:2147483647;font:700 ${px(15)} Roboto,Arial,sans-serif;padding:${px(10)} ${px(14)};border-radius:${px(20)};border:0;background:#38946D;color:#FFFFFF;display:none}`;
 D.head.append(st);
 const bar=D.createElement('div');bar.id='smv';
 const pill=D.createElement('button');pill.id='smvpill';pill.textContent='Herramientas';
 const mk=(txt,cls,down,hold)=>{const b=D.createElement('button');b.textContent=txt;if(cls)b.className=cls;let t1,t2;const stop=()=>{clearTimeout(t1);clearInterval(t2)};
  b.addEventListener('pointerdown',e=>{e.preventDefault();down();if(hold){t1=setTimeout(()=>{t2=setInterval(down,95)},350)}});
  ['pointerup','pointercancel','pointerleave'].forEach(ev=>b.addEventListener(ev,stop));b.addEventListener('click',e=>e.preventDefault());return b};
 const row=(...b)=>{const r=D.createElement('div');r.className='r';r.append(...b);return r};
 bar.append(
  row(mk('Bone','p',()=>key('Numpad0','0',96)),mk('Abdomen','p',()=>key('Numpad1','1',97)),mk('Lung','p',()=>key('Numpad2','2',98)),mk('Inicial','p',()=>key('Numpad9','9',105))),
  row(mk('▲',0,()=>key('ArrowUp','ArrowUp',38),1),mk('▼',0,()=>key('ArrowDown','ArrowDown',40),1),mk('Series',0,openSeries),mk('1 ▢',0,()=>layout('1x1')),mk('2 ▤',0,()=>layout('2x1')),mk('✕',0,()=>{bar.style.display='none';panel.style.display='none';pill.style.display='block';pill.dataset.t=Date.now()})));
 pill.addEventListener('click',()=>{if(Date.now()-(+pill.dataset.t||0)<700)return;bar.style.display='flex';pill.style.display='none'});
 D.body.append(panel,bar,pill);
 window.__SMV={show:()=>{bar.style.display='flex';pill.style.display='none'}};
}
if(/^\/ImageViewer\//i.test(location.pathname)){viewerTools();return}
if(window.__SM){window.__SM.open();return}
const USER=(window.SynapseSignOn&&SynapseSignOn.Session&&SynapseSignOn.Session.UserName)||'BECADO3',ID=encodeURIComponent('/Synapse/All Studies');
const $=(t,a={},...k)=>{const e=document.createElement(t);for(const[x,v]of Object.entries(a)){if(x=='on')for(const[ev,f]of Object.entries(v))e.addEventListener(ev,f);else if(x=='html')e.innerHTML=v;else e.setAttribute(x,v)}k.flat().forEach(c=>c!=null&&e.append(c));return e};
const css=`
#sm{position:fixed;inset:0;z-index:2147483600;background:#122221;color:#E6EEEC;font:16px/1.45 -apple-system,Roboto,"Segoe UI",sans-serif;display:flex;flex-direction:column}
#sm,#sm *{box-sizing:border-box;-webkit-text-size-adjust:100%;text-size-adjust:100%}
#sm .top{background:#0D1A19;padding:10px 12px;border-bottom:1px solid #2E4140;flex:none}
#sm .row{display:flex;gap:8px;align-items:center}
#sm .brand{font-weight:300;color:#FFFFFF;font-size:19px;letter-spacing:3px;flex:1;white-space:nowrap;overflow:hidden}
#sm .brand .hs{color:#9AB0AB;font-size:12px;letter-spacing:1px;font-weight:400}
#sm .x{background:none;border:0;color:#9AB0AB;font-size:26px;padding:0 6px;line-height:1}
#sm input{flex:1 1 auto;width:100%;min-width:0;font-size:20px;padding:11px 12px;border-radius:10px;border:1px solid #2E4140;background:#1B2929;color:#fff;outline:none}
#sm input:focus{border-color:#76D7B5}
#sm .go{font-size:17px;font-weight:700;padding:12px 16px;border-radius:10px;border:0;background:#38946D;color:#FFFFFF;flex:0 0 auto;width:auto}
#sm .chips{display:flex;gap:6px;overflow-x:auto;padding-top:9px;scrollbar-width:none}
#sm .chips::-webkit-scrollbar{display:none}
#sm .chip{flex:none;padding:6px 12px;border-radius:16px;border:1px solid #2E4140;background:#1B2929;color:#9AB0AB;font-size:14px}
#sm .chip.on{background:#38946D;color:#FFFFFF;border-color:#76D7B5;font-weight:700}
#sm .list{flex:1;overflow-y:auto;-webkit-overflow-scrolling:touch;overscroll-behavior:contain;padding:10px 10px 40px}
#sm .pt{padding:4px 4px 10px;color:#9AB0AB;font-size:14px}
#sm .pt b{color:#fff;font-size:17px;display:block}
#sm .card{background:#1B2929;border:1px solid #2E4140;border-radius:12px;margin-bottom:9px;overflow:hidden}
#sm .hd{padding:12px;display:flex;gap:10px;align-items:flex-start}
#sm .mod{flex:none;min-width:44px;text-align:center;font-weight:700;font-size:13px;padding:4px 6px;border-radius:7px;background:#122221;color:#76D7B5;border:1px solid #76D7B5}
#sm .desc{font-weight:600;font-size:16px}
#sm .meta{color:#9AB0AB;font-size:13px;margin-top:2px}
#sm .meta .ok{color:#76D7B5}#sm .meta .no{color:#D7DB83}
#sm .rep{display:none;border-top:1px solid #2E4140;padding:12px;white-space:pre-wrap;font-size:15px;line-height:1.5;background:#162625}
#sm .card.open .rep{display:block}
#sm .acts{display:flex;gap:8px;margin-top:12px}
#sm .btn{flex:1;font-size:15px;font-weight:600;padding:10px;border-radius:9px;border:1px solid #76D7B5;background:none;color:#76D7B5}
#sm .btn.img{background:#38946D;color:#FFFFFF}
#sm .upd{background:none;border:1px solid #2E4140;color:#9AB0AB;border-radius:8px;font-size:12px;padding:4px 8px}
#sm .msg{text-align:center;color:#9AB0AB;padding:40px 16px}
#sm .err{color:#E05A4E}
`;
const norm=s=>String(s||'').toUpperCase().replace(/[^0-9K]/g,'').replace(/^0+/,'');
function dv(b){let s=0,m=2;for(let i=b.length-1;i>=0;i--){s+=+b[i]*m;m=m==7?2:m+1}const r=11-s%11;return r==11?'0':r==10?'K':String(r)}
const post=(p,b)=>fetch('/api/WorkflowEngine/Worklist/'+p+'?identifier='+ID,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(b)}).then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json()});
const mk=v=>({Filters:[{Column:'externalPatientID',Operator:'begins-with',Value:v,Group:1,DataType:'string',SecondValue:null}],Sorts:[{Column:'displayTimestamp',Direction:-1}],Culture:'es-ES',UserLogin:USER,WorklistType:0,TargetDataSources:[]});
async function q(v){const r=await post('Query',Object.assign({ForceRefresh:true},mk(v)));if(!r.Succeeded)return[];const o=[];for(let off=0;off<r.NumResults;off+=500){const d=await post('Retrieve',Object.assign({Length:500,Offset:off,SessionID:r.SessionID},mk(v)));o.push(...d.Records)}return o}
async function search(raw){
 const n=norm(raw);if(n.length<2)throw new Error('RUT incompleto');
 let body=n.slice(0,-1),d=n.slice(-1);
 if(dv(body)!==d){body=n;d=dv(n)} // si escribieron el RUT sin dígito verificador
 const full=body+d,pad=full.padStart(9,'0');
 const vars=[...new Set([body,pad,'0'+body])];
 const res=(await Promise.all(vars.map(q))).flat();
 const seen=new Map();
 res.forEach(x=>{const k=norm(x.externalPatientID);if((k==full||k==body)&&!seen.has(x.instanceEUID))seen.set(x.instanceEUID,x)});
 return [...seen.values()].sort((a,b)=>String(b.displayTimestamp).localeCompare(a.displayTimestamp));
}
async function report(iuid,ds){
 const h=await (await fetch('/WorkflowUI/Worklist/Print?printWidget=reports&studyIuid='+encodeURIComponent(iuid)+'&datasource='+encodeURIComponent(ds||'SJDPACS'))).text();
 if(h.includes('An error occurred'))throw new Error('No se pudo leer el informe');
 const doc=new DOMParser().parseFromString(h,'text/html');let t=doc.body.innerText||doc.body.textContent||'';
 const m=t.match(/Sexo\s*:\s*\n\s*\S*[ \t]*\n/);if(m)t=t.slice(m.index+m[0].length);
 const tmp=document.createElement('div');tmp.innerHTML=t.replace(/<br\s*\/?>/gi,'\n').replace(/<\/p>/gi,'\n').replace(/<(?!br|\/p)[^>]+>/gi,'');
 return (tmp.textContent||'').replace(/[ \t]+/g,' ').replace(/\n\s*\n+/g,'\n\n').trim()||'(Informe vacío)';
}
const fdate=s=>{const d=new Date(s);if(isNaN(d))return s||'';const p=n=>String(n).padStart(2,'0');return p(d.getDate())+'-'+p(d.getMonth()+1)+'-'+d.getFullYear()+' '+p(d.getHours())+':'+p(d.getMinutes())};
// ---------- UI ----------
let rows=[],modSel='Todas';
const inp=$('input',{type:'text',inputmode:'text',autocomplete:'off',autocapitalize:'characters',spellcheck:'false',placeholder:'RUT del paciente'});
const goBtn=$('button',{class:'go'},'Buscar');
const chips=$('div',{class:'chips'});
const list=$('div',{class:'list'},$('div',{class:'msg'},'Ingresa un RUT con o sin puntos y guion.'));
const root=$('div',{id:'sm'},$('style',{},css),
 $('div',{class:'top'},
  $('div',{class:'row',style:'margin-bottom:9px'},$('div',{class:'brand'},'KYL',$('span',{style:'color:#ED1A3A'},'A'),'PSE',$('span',{class:'hs'},' · HSJD')),$('button',{class:'x',title:'Cerrar',on:{click:close}},'×')),
  $('div',{class:'row'},inp,goBtn),chips),list);
function msg(t,err){list.replaceChildren($('div',{class:'msg'+(err?' err':'')},t))}
function renderChips(){const mods=['Todas',...new Set(rows.map(r=>r.procedureModality||'?'))];chips.replaceChildren(...mods.map(m=>$('button',{class:'chip'+(m==modSel?' on':''),on:{click:()=>{modSel=m;renderChips();renderList()}}},m+(m=='Todas'?' ('+rows.length+')':''))))}
function renderList(){
 const sel=rows.filter(r=>modSel=='Todas'||r.procedureModality==modSel);
 const p=rows[0];
 list.replaceChildren($('div',{class:'pt'},$('b',{},p.patientFullName||''),sel.length+' estudio(s)'),...sel.map(card));
 list.scrollTop=0;
}
function card(r){
 const rep=$('div',{class:'rep'});let loaded=false;
 const informed=r.reportStatusCode==30;
 const c=$('div',{class:'card'},
  $('div',{class:'hd',on:{click:async()=>{c.classList.toggle('open');if(c.classList.contains('open')&&!loaded){loaded=true;if(!informed){rep.replaceChildren('Sin informe todavía.',acts(r));return}rep.textContent='Cargando informe…';try{rep.replaceChildren(await report(r.instanceEUID,r.dataSource),acts(r))}catch(e){loaded=false;rep.replaceChildren($('span',{class:'err'},e.message))}}}}},
   $('div',{class:'mod'},r.procedureModality||'?'),
   $('div',{},$('div',{class:'desc'},r.procedureDescription||'(sin descripción)'),
    $('div',{class:'meta'},fdate(r.displayTimestamp)+' · '+(r.studyImageCount||0)+' imágenes · ',$('span',{class:informed?'ok':'no'},informed?'Informado':'Sin informe')))),
  rep);
 return c;
}
function openImgs(r,btn){
 const ds=r.dataSource||'SJDPACS';
 try{dcm.cc.beginTransaction();dcm.cc.setValue('currentPatientUuid',JSON.stringify({patientUuid:r.patientUUID,dataSource:ds}),'StudyChange');dcm.cc.setValue('currentStudyUuid',JSON.stringify({studyInstanceId:r.instanceEUID,dataSource:ds}),'StudyChange');dcm.cc.endTransaction()}
 catch(e){btn.textContent='Abre Kylapse desde la lista de trabajo';return}
 fetch('/ViewerService/api/study/prefetch?studyUid='+encodeURIComponent(r.instanceEUID)+'&dataSource='+encodeURIComponent(ds)).catch(()=>{});
 if(window.__KYL_APP){location.href='/ImageViewer/layout';return}
 const w=window.open('/ImageViewer/layout','smviewer');
 if(!w){btn.textContent='Chrome bloqueó la pestaña: permite ventanas emergentes';return}
 const code='('+viewerTools+')()';let k=0;
 const iv=setInterval(()=>{k++;try{if(w.closed||k>240){clearInterval(iv);return}const d=w.document;if(d.body&&d.querySelector('.TileView_IV')&&!w.__SMV){const s=d.createElement('script');s.textContent=code;d.body.append(s);s.remove();clearInterval(iv)}}catch(e){}},500);
}
function acts(r){
 const b=[];
 if((r.studyImageCount||0)>0)b.push($('button',{class:'btn img',on:{click:e=>{e.stopPropagation();openImgs(r,e.target)}}},'Ver imágenes'));
 b.push($('button',{class:'btn',on:{click:e=>{e.stopPropagation();const t=e.target.closest('.rep').firstChild;navigator.clipboard&&navigator.clipboard.writeText(t.textContent||'');e.target.textContent='Copiado'}}},'Copiar informe'));
 return $('div',{class:'acts'},b)}
async function run(){
 const v=inp.value.trim();if(!v)return;inp.blur();msg('Buscando…');try{sessionStorage.setItem('kyl_rut',v)}catch(e){}
 try{rows=await search(v);modSel='Todas';if(!rows.length){chips.replaceChildren();msg('Sin estudios para ese RUT.');return}renderChips();renderList()}
 catch(e){chips.replaceChildren();msg(/JSON|HTTP 40/.test(e.message)?'La sesión de Synapse expiró. Vuelve a iniciar sesión y abre Kylapse de nuevo.':e.message,true)}
}
goBtn.onclick=run;inp.addEventListener('keydown',e=>{if(e.key=='Enter')run()});
let vp=document.querySelector('meta[name=viewport]'),vpOld=vp&&vp.content;
const reo=$('button',{style:'position:fixed;right:12px;bottom:14px;z-index:2147483600;font:300 15px Roboto,Arial,sans-serif;letter-spacing:2px;padding:10px 16px;border-radius:20px;border:0;background:#38946D;color:#FFFFFF;box-shadow:0 2px 8px rgba(0,0,0,.4)',on:{click:()=>{reo.remove();open()}}},'KYL',$('span',{style:'color:#ED1A3A;font-weight:700'},'A'),'PSE');
function fit(){const sw=(screen&&screen.width)||innerWidth,z=innerWidth/sw;root.style.zoom=z>1.15?String(z):''}
addEventListener('resize',()=>{if(root.isConnected)fit()});
function open(){reo.remove();if(!vp){vp=$('meta',{name:'viewport'});document.head.append(vp)}vp.content='width=device-width,initial-scale=1,maximum-scale=1';document.documentElement.style.overflow='hidden';document.body.append(root);fit();let last='';try{last=sessionStorage.getItem('kyl_rut')||''}catch(e){}if(last&&!rows.length){inp.value=last;run()}else setTimeout(()=>inp.focus(),50)}
function close(){root.remove();document.documentElement.style.overflow='';if(vpOld!=null)vp.content=vpOld;if(window.__KYL_APP)document.body.append(reo)}
window.__SM={open,close,search};open();
})();
