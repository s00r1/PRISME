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
