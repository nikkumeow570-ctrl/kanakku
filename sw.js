const V="kanakku-v6-1",SHELL=["./","index.html","manifest.json","icon-192.png","icon-512.png","icon-maskable-192.png","icon-maskable-512.png"];
self.addEventListener("install",e=>{e.waitUntil(caches.open(V).then(c=>c.addAll(SHELL)).then(()=>self.skipWaiting()));});
self.addEventListener("activate",e=>{e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==V).map(x=>caches.delete(x)))).then(()=>self.clients.claim()));});
self.addEventListener("fetch",e=>{if(e.request.method!=="GET"||new URL(e.request.url).origin!==location.origin)return;
e.respondWith(caches.open(V).then(c=>c.match(e.request).then(hit=>{const net=fetch(e.request).then(r=>{if(r.ok)c.put(e.request,r.clone());return r;}).catch(()=>hit);return hit||net;})));});
self.addEventListener("notificationclick",e=>{e.notification.close();e.waitUntil(clients.matchAll({type:"window"}).then(l=>l.length?l[0].focus():clients.openWindow("./")));});
function idb(){return new Promise((res,rej)=>{const q=indexedDB.open("kanakku",1);q.onupgradeneeded=()=>q.result.createObjectStore("kv");q.onsuccess=()=>res(q.result);q.onerror=()=>rej(q.error);});}
async function idbGet(k){const db=await idb();return new Promise((res,rej)=>{const r=db.transaction("kv").objectStore("kv").get(k);r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error);});}
async function idbSet(k,v){const db=await idb();return new Promise((res,rej)=>{const tx=db.transaction("kv","readwrite");tx.objectStore("kv").put(v,k);tx.oncomplete=()=>res();tx.onerror=()=>rej(tx.error);});}
async function remind(){const st=await idbGet("rem");if(!st||!st.on)return;const n=new Date(),p=x=>String(x).padStart(2,"0"),key=n.getFullYear()+"-"+p(n.getMonth()+1)+"-"+p(n.getDate());const [hh,mm]=String(st.time||"20:00").split(":").map(Number);const reached=n.getHours()>hh||(n.getHours()===hh&&n.getMinutes()>=mm);if(!reached||st.lastEntry===key||st.lastFired===key)return;await self.registration.showNotification(st.title||"Kanakku",{body:st.body||"",icon:"icon-192.png",tag:"kanakku-daily"});st.lastFired=key;await idbSet("rem",st);}
self.addEventListener("periodicsync",e=>{if(e.tag==="kanakku-remind")e.waitUntil(remind());});
