const assert=require('node:assert/strict'),H=require('../website/home-model.js');let checks=0;
assert.equal(H.valid(H.defaults()).pages.length,3);checks++;
for(let x=-1;x<6;x++)for(let y=-1;y<6;y++)for(let w=0;w<6;w++)for(let h=0;h<6;h++){const o=H.defaults();o.pages[0].cards=[{kind:'clock',x,y,w,h}];let valid=true;try{H.valid(o)}catch{valid=false}assert.equal(valid,x>=0&&x<4&&y>=0&&y<4&&w>=1&&h>=1&&x+w<=4&&y+h<=4);checks++;}
const collision=H.defaults();collision.pages[0].cards.push({...collision.pages[0].cards[0]});assert.throws(()=>H.valid(collision));checks++;
for(const mutation of [o=>o.version=2,o=>o.theme=6,o=>o.largeText='yes',o=>o.pages=[],o=>o.pages[0].name='\n',o=>o.pages[0].cards[0].kind='script',o=>o.pages[0].cards[0].x=.5,o=>o.code='bad']){const o=H.defaults();mutation(o);assert.throws(()=>H.valid(o));checks++;}
const a=H.defaults(),b=H.valid(a);b.pages[0].name='Changed';assert.notEqual(a.pages[0].name,b.pages[0].name);checks++;console.log('HomeModel: '+checks+' assertions passed.');
