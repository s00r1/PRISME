'use strict';
// Both editors require the complete export shape. Android intentionally accepts
// partial legacy imports; an editor must never turn one into a replacement backup.
const PrismeProfileModel=(()=>{
 const sets=['favorites','hidden','workspace1','workspace2','workspace3'];
 // These fields are emitted by every released V5 schema-6 export (v5.1 through
 // v5.4). Later additions stay optional so genuine V5 files remain supported.
 const settingsInts=['theme','iconSize','textSize','columns','gap','wallpaper','iconShape','order','workspace','font','accentIndex','styleScale','contentInset','fitMode','upGesture','blankDoubleTap','language'];
 const settingsBools=['labels','animations','favoritesOnly','showClock','showBattery','showDock','haptic','keepScreenOn','volumeRail','edgeCards','compactNavigation','deskKeepAwake','styleDock'];
 const centerInts=['style','opacity','scale','columns','position','width','height','edge','opening','closing','edgeSize','swipeDistance','handlePosition','radius','accent','customColor','tileOpacity','scrimOpacity','closeDistance'];
 const centerBools=['enabled','dark','labels','status','volume','brightness','customAccent','compactHandle'];
 const volumeInts=['style','shape','layout','size','opacity','expandedOpacity','accent','customColor','stream','xPercent','yPercent','collapseDelay','repeatDelay','repeatInterval','idleDelay','idleOpacity'];
 const volumeBools=['customAccent','snap','locked','autoCollapse','showLevel','showMute','haptic','idleFade'];
 const object=value=>value!==null&&typeof value==='object'&&!Array.isArray(value);
 function fail(){throw Error('Export PRISME V5/V6 complet requis : réexporter la configuration depuis la montre.');}
 function fields(value,ints,bools){if(!object(value))fail();for(const key of ints)if(!Number.isInteger(value[key]))fail();for(const key of bools)if(typeof value[key]!=='boolean')fail();}
 function validateComplete(data){
  if(!object(data)||![6,7].includes(data.schemaVersion)||typeof data.notes!=='string'||data.notes.length>2000)fail();
  fields(data.settings,settingsInts,settingsBools);
  if(data.settings.downGesture!==undefined&&!Number.isInteger(data.settings.downGesture))fail();
  for(const key of sets)if(!Array.isArray(data[key])||data[key].some(value=>typeof value!=='string'))fail();
  for(const key of ['customLabels','launches','shortcuts'])if(!object(data[key]))fail();
  if(!Array.isArray(data.organizer)||data.organizer.length>40)fail();
  const ids=new Set();for(const item of data.organizer){if(!object(item)||typeof item.id!=='string'||!item.id||item.id.length>64||ids.has(item.id)||typeof item.text!=='string'||!item.text.trim()||item.text.length>120||typeof item.done!=='boolean')fail();ids.add(item.id);}
  if(!object(data.controlCenter)||!Array.isArray(data.controlCenter.tiles))fail();
  fields(data.controlCenter.settings,centerInts,centerBools);
  if(data.controlCenter.settings.media!==undefined&&typeof data.controlCenter.settings.media!=='boolean')fail();
  if(!object(data.floatingVolume))fail();fields(data.floatingVolume.settings,volumeInts,volumeBools);
  if(data.schemaVersion===7){if(!object(data.v6))fail();for(const key of ['battery','music','studio','sense','widgets','edge'])if(!object(data.v6[key]))fail();}
  else if(data.v6!==undefined)fail();
  if(new TextEncoder().encode(JSON.stringify(data)).length>65536)fail();
  return structuredClone(data);
 }
 return {validateComplete};
})();
if(typeof module!=='undefined')module.exports=PrismeProfileModel;
