const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const elements=new Map(),requests=[];
function el(){return {value:'fr',dataset:{},open:false,append(){},replaceChildren(){},setAttribute(){},addEventListener(){},showModal(){assert(!this.open);this.open=true},close(){this.open=false}}}
const document={querySelector(s){if(!elements.has(s))elements.set(s,el());return elements.get(s)},querySelectorAll(){return []},createElement:el};
const context=vm.createContext({document,fetch:url=>new Promise(resolve=>requests.push({url,resolve}))});
vm.runInContext(fs.readFileSync('website/app.js','utf8'),context);
const run=s=>vm.runInContext(s,context),tick=()=>new Promise(r=>setImmediate(r));
(async()=>{
 requests.shift().resolve({ok:true,json:async()=>[{file:'one.png',name:'One'}]});await tick();assert.equal(run('all.length'),1);
 run("openImage('one.png','One');openImage('two.png','Two')");assert.equal(document.querySelector('#large').alt,'Two');
 run("loadGallery('en');loadGallery('de')");let [en,de]=requests.splice(0);
 de.resolve({ok:true,json:async()=>[{file:'de.png',name:'Deutsch'}]});await tick();en.resolve({ok:true,json:async()=>[{file:'en.png',name:'English'}]});await tick();assert.equal(run('all[0].name'),'Deutsch');
 run("loadGallery('fr')");requests.shift().resolve({ok:false,status:404});await tick();assert.equal(run('all.length'),0);assert.match(document.querySelector('#count').textContent,/Impossible/);
 run("loadGallery('fr')");requests.shift().resolve({ok:true,json:async()=>({broken:true})});await tick();assert.equal(run('shown.length'),0);
 run("loadGallery('fr')");requests.shift().resolve({ok:true,json:async()=>[{file:'ok.png',name:'Recovered'}]});await tick();assert.equal(run('all[0].name'),'Recovered');
 console.log('PASS: gallery loading, concurrent languages, HTTP/invalid data errors, retry, modal navigation');
})().catch(e=>{console.error(e);process.exitCode=1});
