'use strict';
const HomeModel=(()=>{
 const kinds=['clock','tasks','timer','music','notes','favorites','device'];
 const card=(kind,x,y,w,h)=>({kind,x,y,w,h});
 function defaults(){return {version:1,theme:0,largeText:false,pages:[{name:'Principal',cards:[card('clock',0,0,2,2),card('tasks',2,0,2,1),card('music',2,1,2,1),card('timer',0,2,2,1),card('notes',2,2,2,1),card('favorites',0,3,4,1)]},{name:'Travail',cards:[card('clock',0,0,2,1),card('timer',2,0,2,1),card('tasks',0,1,4,2),card('notes',0,3,4,1)]},{name:'Multimédia',cards:[card('clock',0,0,2,1),card('device',2,0,2,1),card('music',0,1,4,2),card('favorites',0,3,4,1)]}]};}
 function valid(o){
  const keys=(x,allowed)=>{if(!x||typeof x!=='object'||Array.isArray(x)||Object.keys(x).some(k=>!allowed.includes(k)))throw Error('Option de disposition inconnue');};
  const integer=(v,min,max)=>{if(!Number.isInteger(v)||v<min||v>max)throw Error('Valeur hors limites');};
  keys(o,['version','theme','largeText','pages']);integer(o.version,1,1);integer(o.theme,0,5);if(typeof o.largeText!=='boolean'||!Array.isArray(o.pages)||o.pages.length<1||o.pages.length>5)throw Error('Pages invalides');
  for(const p of o.pages){keys(p,['name','cards']);if(typeof p.name!=='string'||!p.name.trim()||p.name.length>24||/[\u0000-\u001f\u007f-\u009f]/.test(p.name)||!Array.isArray(p.cards)||p.cards.length>8)throw Error('Page invalide');const cells=new Set();for(const c of p.cards){keys(c,['kind','x','y','w','h']);if(!kinds.includes(c.kind))throw Error('Bloc inconnu');integer(c.x,0,3);integer(c.y,0,3);integer(c.w,1,4);integer(c.h,1,4);if(c.x+c.w>4||c.y+c.h>4)throw Error('Bloc hors écran');for(let x=c.x;x<c.x+c.w;x++)for(let y=c.y;y<c.y+c.h;y++){const k=x+','+y;if(cells.has(k))throw Error('Blocs superposés');cells.add(k);}}}
  if(JSON.stringify(o).length>12000)throw Error('Disposition trop grande');return structuredClone(o);
 }
 return {kinds,defaults,valid};
})();
if(typeof module!=='undefined')module.exports=HomeModel;


// Portable appearance for the common widget area. Consent, city and cached forecasts
// are deliberately absent: those remain device-local on the watch.
const DashboardWidgetModel = (() => {
  const types = ['weather','clock','music','battery','tasks','notes','timers','calendar','world','system','focus','shortcuts','stopwatch'];
  const names = ['Météo','Horloge','Musique','Batterie','Tâches','Carnet','Minuteurs','Calendrier','Horloge mondiale','Système','Concentration','Raccourcis','Chronomètre'];
  const skins = ['Signature du bureau','Verre','Carte pleine','Contour','Minimal','Néon'];
  function widget(type,enabled=false,id=type) {
    return {id,type,enabled,skin:0,size:1,opacity:100,radius:18,accent:0,textScale:100,title:'',showTitle:true,seconds:false,hour24:true,detail:true,variant:0,timezone:'UTC',themes:[]};
  }
  function defaults() {
    return {version:1,side:1,width:176,widgets:['weather','music','shortcuts','clock','battery','tasks','timers','calendar','notes','world','system','focus'].map(type=>widget(type,['weather','music','tasks'].includes(type)))};
  }
  function validate(o) {
    const keys=(value,allowed)=>{if(!value||typeof value!=='object'||Array.isArray(value)||Object.keys(value).some(k=>!allowed.includes(k)))throw Error('Option de widget inconnue');};
    const integer=(value,min,max)=>{if(!Number.isInteger(value)||value<min||value>max)throw Error('Valeur de widget hors limites');};
    const cleanText=(value,max)=>{if(typeof value!=='string'||value.length>max||/[\u0000-\u001f\u007f-\u009f]/.test(value))throw Error('Texte de widget invalide');};
    keys(o,['version','side','width','widgets']);integer(o.version,1,1);integer(o.side,0,2);integer(o.width,144,208);
    if(!Array.isArray(o.widgets)||o.widgets.length>16)throw Error('16 widgets maximum');
    const ids=new Set();
    for(const w of o.widgets) {
      keys(w,['id','type','enabled','skin','size','opacity','radius','accent','textScale','title','showTitle','timezone','themes','seconds','hour24','detail','variant']);
      if(typeof w.id!=='string'||! /^[A-Za-z0-9_-]{1,40}$/.test(w.id)||ids.has(w.id)||!types.includes(w.type))throw Error('Identifiant ou type de widget invalide');ids.add(w.id);
      for(const key of ['enabled','showTitle','seconds','hour24','detail'])if(typeof w[key]!=='boolean')throw Error('Option de widget invalide');
      integer(w.skin,0,5);integer(w.size,0,2);integer(w.opacity,30,100);integer(w.radius,0,28);integer(w.accent,0,6);integer(w.textScale,80,140);integer(w.variant,0,3);
      cleanText(w.title,40);cleanText(w.timezone,80);if(!w.timezone)throw Error('Fuseau horaire requis');
      if(!Array.isArray(w.themes)||w.themes.length>22||new Set(w.themes).size!==w.themes.length)throw Error('Liste de styles invalide');for(const n of w.themes)integer(n,0,21);
    }
    if(JSON.stringify(o).length>24000)throw Error('Configuration de widgets trop grande');
    return structuredClone(o);
  }
  const visible=(w,theme)=>w.enabled&&(w.themes.length===0||w.themes.includes(theme));
  return {types,names,skins,widget,defaults,validate,visible};
})();
HomeModel.widgets=DashboardWidgetModel;
