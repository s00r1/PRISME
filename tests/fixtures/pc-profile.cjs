'use strict';
// Fictional, complete export shapes. V5 keys match released v5.1.0/v5.4;
// optional downGesture and Control Center media appeared later in V6.
module.exports=function fixture(schema=7){
 const settings={theme:0,iconSize:72,textSize:16,columns:5,gap:12,wallpaper:0,iconShape:0,order:0,workspace:0,font:0,accentIndex:0,styleScale:100,contentInset:0,fitMode:0,upGesture:1,blankDoubleTap:1,language:0,labels:true,animations:true,favoritesOnly:false,showClock:true,showBattery:true,showDock:true,haptic:true,keepScreenOn:false,volumeRail:false,edgeCards:true,compactNavigation:false,deskKeepAwake:false,styleDock:false};
 const center={style:0,opacity:68,scale:100,columns:4,position:0,width:100,height:90,edge:0,opening:3,closing:0,edgeSize:64,swipeDistance:36,handlePosition:1,radius:24,accent:0,customColor:0x62d9cc,tileOpacity:28,scrimOpacity:8,closeDistance:28,enabled:true,dark:true,labels:true,status:true,volume:true,brightness:true,customAccent:false,compactHandle:true};
 const volume={style:0,shape:0,layout:0,size:56,opacity:90,expandedOpacity:95,accent:0,customColor:0x62d9cc,stream:0,xPercent:100,yPercent:55,collapseDelay:4,repeatDelay:450,repeatInterval:140,idleDelay:8,idleOpacity:40,customAccent:false,snap:true,locked:false,autoCollapse:true,showLevel:true,showMute:true,haptic:true,idleFade:true};
 const profile={schemaVersion:schema,settings,notes:'Synthetic private note to preserve',organizer:[{id:'qa_task',text:'Synthetic task to preserve',done:false}],favorites:['org.example.demo/.Main'],hidden:[],workspace1:['org.example.demo/.Main'],workspace2:[],workspace3:[],customLabels:{'org.example.demo/.Main':'Demo app'},launches:{'org.example.demo/.Main':1800000000},shortcuts:{'0':'apps'},controlCenter:{settings:center,tiles:['wifi','bluetooth','settings']},floatingVolume:{settings:volume}};
 if(schema===7){profile.settings.downGesture=1;profile.controlCenter.settings.media=true;profile.v6={battery:{},music:{},studio:{},sense:{},widgets:{},edge:{}};}
 return profile;
};
