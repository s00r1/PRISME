'use strict';
const assert=require('node:assert/strict');
const W=require('../website/home-model.js').widgets;
let checks=0;
const ok=(value,expected)=>{assert.deepEqual(value,expected);checks++;};
const rejects=mutation=>{const config=W.defaults();mutation(config);assert.throws(()=>W.validate(config));checks++;};
ok(W.types.length,13);ok(new Set(W.types).size,13);ok(W.validate(W.defaults()).widgets.filter(w=>w.enabled).map(w=>w.type),['weather','music','tasks']);
for(const type of W.types){const config=W.defaults();config.widgets=[W.widget(type,true)];ok(W.validate(config).widgets[0].type,type);}
for(const [field,min,max]of [['side',0,2],['width',144,208]])for(const value of [min-1,min,max,max+1,1.1,'1',null]){const config=W.defaults();config[field]=value;const valid=Number.isInteger(value)&&value>=min&&value<=max;if(valid)ok(W.validate(config)[field],value);else rejects(o=>o[field]=value);}
for(const [field,min,max]of [['skin',0,5],['size',0,2],['opacity',30,100],['radius',0,28],['accent',0,6],['textScale',80,140],['variant',0,3]])for(const value of [min-1,min,max,max+1,1.1,'1',null]){const config=W.defaults();config.widgets[0][field]=value;if(Number.isInteger(value)&&value>=min&&value<=max)ok(W.validate(config).widgets[0][field],value);else rejects(o=>o.widgets[0][field]=value);}
for(const field of ['enabled','showTitle','seconds','hour24','detail'])for(const value of [0,1,'true',null])rejects(o=>o.widgets[0][field]=value);
for(const mutation of [o=>o.version=2,o=>o.unknown=true,o=>o.widgets[0].unknown=true,o=>delete o.widgets[0].type,o=>o.widgets[0].type='fetch',o=>o.widgets[0].id='bad id',o=>o.widgets[0].id='x'.repeat(41),o=>o.widgets[1].id=o.widgets[0].id,o=>o.widgets[0].title='x'.repeat(41),o=>o.widgets[0].title='bad\u0000title',o=>o.widgets[0].title='bad\u0085title',o=>o.widgets[0].timezone='',o=>o.widgets[0].themes=[0,0],o=>o.widgets[0].themes=[22],o=>o.widgets[0].themes=['0'],o=>o.widgets[0].themes=[null],o=>o.widgets=Array.from({length:17},(_,i)=>W.widget('clock',true,'clock'+i))])rejects(mutation);
const empty=W.defaults();empty.widgets=[];ok(W.validate(empty).widgets,[]);const full=W.defaults();full.widgets=Array.from({length:16},(_,i)=>W.widget('clock',true,'clock'+i));ok(W.validate(full).widgets.length,16);
const original=W.defaults(),validated=W.validate(original);validated.widgets[0].title='Changed';ok(original.widgets[0].title,'');const w=W.widget('clock',true);ok(W.visible(w,21),true);w.themes=[1,3];ok(W.visible(w,1),true);ok(W.visible(w,2),false);w.enabled=false;ok(W.visible(w,1),false);
console.log('DashboardWidgetModel: '+checks+' assertions passed.');
