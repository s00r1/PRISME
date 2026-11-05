'use strict';
const assert=require('node:assert/strict'),P=require('../website/profile-model.js'),fixture=require('./fixtures/pc-profile.cjs');
let count=0;
function valid(profile){assert.deepEqual(P.validateComplete(profile),profile);count++;}
function reject(change,schema=7){const profile=fixture(schema),before=structuredClone(profile);change(profile);const mutated=structuredClone(profile);assert.throws(()=>P.validateComplete(profile));assert.deepEqual(profile,mutated);count+=2;return before;}
valid(fixture(6));valid(fixture(7));
for(const schema of [6,7])for(const key of Object.keys(fixture(schema)))reject(p=>delete p[key],schema);
for(const group of ['settings','controlCenter','floatingVolume','customLabels','launches','shortcuts'])for(const value of [null,[],false,'partial'])reject(p=>p[group]=value);
for(const path of [['settings'],['controlCenter','settings'],['floatingVolume','settings']])for(const key of Object.keys(path.reduce((o,k)=>o[k],fixture(6))))reject(p=>{delete path.reduce((o,k)=>o[k],p)[key];});
for(const key of ['favorites','hidden','workspace1','workspace2','workspace3']){reject(p=>p[key]=null);reject(p=>p[key]=[42]);}
for(const value of [null,42,'x'.repeat(2001)])reject(p=>p.notes=value);
reject(p=>p.organizer[0].done='false');reject(p=>p.organizer.push({...p.organizer[0]}));reject(p=>p.settings.downGesture='1');reject(p=>p.controlCenter.settings.media='true');reject(p=>p.v6={});reject(p=>p.v6={},6);
const emptyData=fixture(6);emptyData.notes='';emptyData.organizer=[];for(const key of ['favorites','hidden','workspace1','workspace2','workspace3'])emptyData[key]=[];emptyData.customLabels={};emptyData.launches={};emptyData.shortcuts={};valid(emptyData);
const source=fixture(),clean=P.validateComplete(source);clean.notes='Changed';assert.notEqual(source.notes,clean.notes);count++;clean.organizer[0].text='Changed';assert.notEqual(source.organizer[0].text,clean.organizer[0].text);count++;
const converted=P.validateComplete(fixture(6));converted.schemaVersion=7;converted.v6={battery:{},music:{},studio:{},sense:{},widgets:{},edge:{}};valid(converted);assert.equal(converted.settings.downGesture,undefined);assert.equal(converted.controlCenter.settings.media,undefined);count+=2;
console.log('CompleteProfileModel: '+count+' assertions passed.');
