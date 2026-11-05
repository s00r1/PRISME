'use strict';
// PRISME Studio V6. No analytics, HTTP requests, remote dependencies or eval.
const THEMES = [{"id":0,"name":"Prisme Touch","background":"#101A38","background2":"#503375","surface":"#344164","accent":"#9CC9FF"},{"id":1,"name":"Tablette","background":"#091D2C","background2":"#17505D","surface":"#193B4B","accent":"#5CE4CE"},{"id":2,"name":"Bureau Windows","background":"#071B37","background2":"#0D5EAA","surface":"#153959","accent":"#45CCFF"},{"id":3,"name":"Dock macOS","background":"#241C3F","background2":"#A04D74","surface":"#513C61","accent":"#FFCBA4"},{"id":4,"name":"Kali Workbench","background":"#07101D","background2":"#122941","surface":"#132333","accent":"#3DBDFF"},{"id":5,"name":"Ghost Terminal","background":"#030B08","background2":"#08251A","surface":"#092016","accent":"#62FFA5"},{"id":6,"name":"Arcade 1986","background":"#190A32","background2":"#36174A","surface":"#281344","accent":"#FFE867"},{"id":7,"name":"Meca Cockpit","background":"#15191E","background2":"#303842","surface":"#252D35","accent":"#FFBD58"},{"id":8,"name":"Orbit","background":"#050B24","background2":"#151647","surface":"#181C43","accent":"#B9A2FF"},{"id":9,"name":"Papier","background":"#F2EEE5","background2":"#DED8CB","surface":"#FFFFFF","accent":"#855D3A"},{"id":10,"name":"Aurora","background":"#071E28","background2":"#302047","surface":"#183846","accent":"#83FFE2"},{"id":11,"name":"LCD Studio","background":"#CED8BD","background2":"#AEBE99","surface":"#DCE4CC","accent":"#34462C"},{"id":12,"name":"Apple Ultra","background":"#050608","background2":"#1B2025","surface":"#171C21","accent":"#FF7B21"},{"id":13,"name":"Samsung Galaxy","background":"#070910","background2":"#262239","surface":"#202236","accent":"#B4ABFF"},{"id":14,"name":"Garmin MARQ","background":"#0A1111","background2":"#152727","surface":"#1B2928","accent":"#D9CC91"},{"id":15,"name":"TAG Racing","background":"#0B0C0F","background2":"#282C32","surface":"#1E2229","accent":"#FF4D50"},{"id":16,"name":"Hublot Fusion","background":"#090E13","background2":"#222D36","surface":"#1B252E","accent":"#5BE0D0"},{"id":17,"name":"Tambour Light","background":"#241522","background2":"#563246","surface":"#493343","accent":"#EAC68F"},{"id":18,"name":"Montblanc Summit","background":"#F2F0E8","background2":"#DADDD9","surface":"#FFFFFF","accent":"#314A65"},{"id":19,"name":"Ultimate Design","background":"#061128","background2":"#162E59","surface":"#13264A","accent":"#E7BF68"},{"id":20,"name":"Nova","background":"#07121C","background2":"#143342","surface":"#173340","accent":"#85E9CF"},{"id":21,"name":"Silence","background":"#000000","background2":"#080A0D","surface":"#18202A","accent":"#BCD9FF"}];
const DEFAULT_X=[3,57,3,57], DEFAULT_Y=[10,10,75,75];
const KEYS_X=['clockX','tasksX','timerX','infoX'],KEYS_Y=['clockY','tasksY','timerY','infoY'];
const SHOW=['showClock','showTasks','showTimer','showDevice'];
const $ = id=>document.getElementById(id);
const preview=$('preview'), nodes=[...document.querySelectorAll('[data-widget]')];
let profile=null;
let model={theme:0,size:75,opacity:88,shown:[true,true,true,true],x:[...DEFAULT_X],y:[...DEFAULT_Y]};
const clamp=(n,lo,hi)=>Math.min(hi,Math.max(lo,n));
const asNumber=(n,defaultValue)=>Number.isFinite(Number(n))?Number(n):defaultValue;
function report(message){$('status').textContent=message;}
function paint(){
  const t=THEMES[clamp(Math.round(model.theme),0,21)];
  preview.style.setProperty('--watch-a',t.background);
  preview.style.setProperty('--watch-b',t.background2);
  preview.style.setProperty('--watch-accent',t.accent);
  $('themeName').textContent=t.name;
  $('theme').value=String(model.theme);
  $('size').value=String(model.size);$('sizeValue').textContent=String(model.size);
  $('opacity').value=String(model.opacity);$('opacityValue').textContent=String(model.opacity);
  for(let i=0;i<4;i++){
    const c=nodes[i],w=clamp(22*model.size/75,14,58),h=clamp(11*model.size/75,8,36);
    c.hidden=!model.shown[i]; c.style.width=w+'%';c.style.height=h+'%';
    c.style.left=((100-w)*clamp(model.x[i],0,100)/100)+'%';
    c.style.top=((100-h)*clamp(model.y[i],0,100)/100)+'%';
    c.style.opacity=String(model.opacity/100);
    c.style.fontSize=clamp(model.size/5.3,11,28)+'px';
    document.querySelector('[data-show="'+SHOW[i]+'"]').checked=model.shown[i];
  }
}
function loadModel(data){
  const d=data.v6||{},studio=d.studio||{},widgets=d.widgets||{};
  model.theme=clamp(Math.round(asNumber(data.settings.theme,0)),0,21);
  model.size=clamp(Math.round(asNumber(widgets.size,75)),30,200);
  model.opacity=clamp(Math.round(asNumber(widgets.opacity,88)),20,100);
  for(let i=0;i<4;i++){
    model.x[i]=clamp(Math.round(asNumber(studio[KEYS_X[i]],DEFAULT_X[i])),0,100);
    model.y[i]=clamp(Math.round(asNumber(studio[KEYS_Y[i]],DEFAULT_Y[i])),0,100);
    model.shown[i]=Boolean(widgets[SHOW[i]]??false);
  }
}
for(const t of THEMES){const opt=document.createElement('option');opt.value=String(t.id);opt.textContent=t.name;$('theme').append(opt);}
$('theme').addEventListener('change',e=>{model.theme=Number(e.target.value);paint();});
for(let i=0;i<4;i++)document.querySelector('[data-show="'+SHOW[i]+'"]').addEventListener('change',e=>{model.shown[i]=e.target.checked;paint();});
for(const key of ['size','opacity'])$(key).addEventListener('input',e=>{model[key]=Number(e.target.value);paint();});
$('reset').addEventListener('click',()=>{model.x=[...DEFAULT_X];model.y=[...DEFAULT_Y];paint();report('Positions réinitialisées sur l’aperçu.');});
$('profileInput').addEventListener('change',async e=>{
  const file=e.target.files&&e.target.files[0];if(!file)return;
  if(file.size>65536){report('Fichier trop volumineux, maximum 64 Ko.');return;}
  try {
    const data=JSON.parse(await file.text());
    if(!data||!Number.isInteger(data.schemaVersion)||![6,7].includes(data.schemaVersion)||
        !data.settings||!data.controlCenter||!data.floatingVolume||!Array.isArray(data.organizer)){
      throw Error('Un export PRISME récent (schéma 6 ou 7) est nécessaire.');
    }
    profile=data;
    loadModel(data);paint();
    $('export').disabled=false;report('Profil chargé localement : prêt pour les modifications et l’export.');
  }catch(err){profile=null;$('export').disabled=true;report('Import refusé : '+(err?.message||'JSON invalide'));}
});
for(let i=0;i<4;i++){
  const card=nodes[i];let grip=null;
  card.addEventListener('pointerdown',e=>{
    if(!model.shown[i])return;
    e.preventDefault();card.setPointerCapture(e.pointerId);
    const rect=preview.getBoundingClientRect(),box=card.getBoundingClientRect();
    grip={dx:e.clientX-box.left,dy:e.clientY-box.top,parent:rect};
  });
  card.addEventListener('pointermove',e=>{
    if(!grip)return;e.preventDefault();
    const w=card.offsetWidth,h=card.offsetHeight;
    const availableX=Math.max(1,grip.parent.width-w),availableY=Math.max(1,grip.parent.height-h);
    const xp=clamp(Math.round(100*(e.clientX-grip.parent.left-grip.dx)/availableX/5)*5,0,100);
    const yp=clamp(Math.round(100*(e.clientY-grip.parent.top-grip.dy)/availableY/5)*5,0,100);
    model.x[i]=xp;model.y[i]=yp;paint();
  });
  const release=()=>{grip=null;};
  card.addEventListener('pointerup',release);card.addEventListener('pointercancel',release);
  card.addEventListener('keydown',e=>{
    if(!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(e.key))return;
    e.preventDefault();if(e.key==='ArrowLeft')model.x[i]=clamp(model.x[i]-5,0,100);
    if(e.key==='ArrowRight')model.x[i]=clamp(model.x[i]+5,0,100);
    if(e.key==='ArrowUp')model.y[i]=clamp(model.y[i]-5,0,100);
    if(e.key==='ArrowDown')model.y[i]=clamp(model.y[i]+5,0,100);
    paint();
  });
}
$('export').addEventListener('click',()=>{
  if(!profile)return;
  // Copy the user's own backup, never generate incomplete or unvalidated settings.
  const data=JSON.parse(JSON.stringify(profile));
  data.schemaVersion=7;data.v6=data.v6||{};
  for(const group of ['battery','music','studio','sense','widgets','edge'])data.v6[group]=data.v6[group]||{};
  data.settings.theme=model.theme;
  data.v6.widgets.size=model.size;data.v6.widgets.opacity=model.opacity;
  for(let i=0;i<4;i++){
    data.v6.studio[KEYS_X[i]]=model.x[i];
    data.v6.studio[KEYS_Y[i]]=model.y[i];
    data.v6.widgets[SHOW[i]]=model.shown[i];
  }
  // Do not move any secret profile data to a server.
  const body=JSON.stringify(data,null,2);
  if(new TextEncoder().encode(JSON.stringify(data)).length>65536){report('Export supérieur à 64 Ko, refusé.');return;}
  const blob=new Blob([body],{type:'application/json;charset=utf-8'}),url=URL.createObjectURL(blob);
  const a=document.createElement('a');a.href=url;a.download='PRISME-V6-Studio.json';a.click();
  setTimeout(()=>URL.revokeObjectURL(url),1500);
  report('Profil exporté. Importe-le ensuite depuis PRISME sur la montre.');
});
paint();
