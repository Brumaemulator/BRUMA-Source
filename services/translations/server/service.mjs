import { createHash, createHmac, randomBytes, timingSafeEqual } from 'node:crypto';
export const sha = b => createHash('sha256').update(b).digest('hex');
const idOK = s => typeof s === 'string' && /^[a-z0-9][a-z0-9-]{0,63}$/.test(s);
const hashOK = s => typeof s === 'string' && /^[a-f0-9]{64}$/.test(s);
const equal = (a,b) => typeof a === 'string' && typeof b === 'string' && a.length===b.length && timingSafeEqual(Buffer.from(a),Buffer.from(b));
const fail = (status,message) => { throw Object.assign(new Error(message),{status}); };
const headers = {'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'};
const json = (body,status=200) => new Response(JSON.stringify(body),{status,headers:{...headers,'Content-Type':'application/json'}});
export function createService(store, env, now=()=>Date.now()) {
 const sign = s => createHmac('sha256',env.DOWNLOAD_SIGNING_KEY).update(s).digest('hex');
 const bearer = req => req.headers.get('authorization')?.replace(/^Bearer /,'') || '';
 const admin = req => { if(!env.ADMIN_TOKEN || !equal(bearer(req),env.ADMIN_TOKEN))fail(401,'Admin authorization required'); };
 const writer = req => {const token=bearer(req);if(!(env.ADMIN_TOKEN && equal(token,env.ADMIN_TOKEN)) && !(env.CREATOR_TOKEN && equal(token,env.CREATOR_TOKEN)))fail(401,'Approved creator authorization required');};
 const device = async req => {
  const token=bearer(req);
  if(token.startsWith('v2.')){
   const parts=token.split('.');const expires=Number(parts[1]);
   if(parts.length!==4 || !/^[a-f0-9]{48}$/.test(parts[2]) || !Number.isSafeInteger(expires) || expires<=now() || expires>now()+3600000 || !equal(parts[3],sign('session:'+parts[1]+':'+parts[2])))fail(401,'Authorization expired or invalid');
   const id=sha(token);if((await store.get('device/'+id))?.revoked)fail(401,'Authorization revoked');return id;
  }
  if(!/^[a-f0-9]{64}$/.test(token))fail(401,'BRUMA authorization required');
  const record=await store.get('device/'+sha(token));if(!record || record.expires<now() || record.revoked)fail(401,'Authorization expired or revoked');
  return sha(token);
 };
 const body = async req => {
  const text=await req.text();if(Buffer.byteLength(text)>3*1024*1024)fail(413,'Payload too large');
  try{return JSON.parse(text);}catch{fail(400,'Invalid JSON');}
 };
 return async req => {
  try {
   const u=new URL(req.url),path=u.pathname.replace(/^\/\.netlify\/functions\/translations/,'/api/translations');
   const origin=req.headers.get('origin');
   if(req.method!=='GET' && origin && origin!==u.origin)fail(403,'Cross-origin write rejected');
   if(path==='/api/translations/access' && req.method==='POST') {
    // Public native-client protocol, NOT attestation or proof of a paid purchase.
    // Headers can be imitated; signed short-lived grants protect download URLs.
    if(origin || req.headers.has('sec-fetch-mode') || req.headers.get('x-bruma-client')!=='android' || !/^BRUMA\/\d+ Android$/.test(req.headers.get('user-agent')||''))fail(403,'Native client required');
    if(req.headers.get('content-type')!=='application/json')fail(400,'JSON required');
    const text=await req.text();if(Buffer.byteLength(text)>4096)fail(413,'Payload too large');
    let b;try{b=JSON.parse(text)}catch{fail(400,'Invalid JSON')}
    if(b?.package!=='com.brumastudio.bruma' || !Number.isSafeInteger(b.versionCode) || b.versionCode<44 || b.versionCode>2147483647)fail(400,'Unsupported client');
    const expires=now()+3600000,nonce=randomBytes(24).toString('hex');
    return json({token:`v2.${expires}.${nonce}.${sign('session:'+expires+':'+nonce)}`,expiresIn:3600});
   }
   if(path==='/api/translations/catalog' && req.method==='GET') {
    const records=await store.list('translation/');
    return json({schema:1,translations:records.filter(x=>x.approved).map(({objectKey,...x})=>x)});
   }
   if(path==='/api/translations/admin/catalog' && req.method==='GET') {admin(req);return json({translations:await store.list('translation/')});}
   if(path==='/api/translations/admin/upload' && req.method==='POST') {
    writer(req);const b=await body(req),m=b.metadata;
    if(!m || !idOK(m.id) || !['GBA','GBC','GB','NDS'].includes(m.system) || !hashOK(m.gameSha256) || !hashOK(m.targetSha256))fail(400,'Invalid metadata');
    for(const field of ['game','language','author','version','notes'])if(typeof m[field]!=='string' || m[field].length>2000 || (field!=='notes' && !m[field].trim()))fail(400,'Missing '+field);
    if(!['IPS','BPS','UPS','XDELTA'].includes(m.patchType) || b.rightsConfirmed!==true)fail(400,'Patch type and redistribution authorization required');
    if(m.patchType==='XDELTA' && (!Number.isSafeInteger(m.targetBytes) || m.targetBytes<1 || m.targetBytes>512*1024*1024))fail(400,'xdelta requires targetBytes');
    if(typeof b.patchBase64!=='string' || !/^[A-Za-z0-9+/]*={0,2}$/.test(b.patchBase64))fail(400,'Invalid patch encoding');
    const data=Buffer.from(b.patchBase64,'base64');if(!data.length || data.length>2*1024*1024)fail(413,'Patch maximum 2 MiB for this pilot');
    const magic={IPS:'PATCH',BPS:'BPS1',UPS:'UPS1',XDELTA:'\xd6\xc3\xc4'}[m.patchType];
    if(!data.subarray(0,magic.length).equals(Buffer.from(magic,'latin1')))fail(400,'Patch header mismatch; games are not accepted');
    if(await store.get('translation/'+m.id))fail(409,'Immutable ID already exists; create a new version ID');
    const patchSha256=sha(data),objectKey='patch/'+patchSha256;
    await store.setBytes(objectKey,data);
    const record={id:m.id,game:m.game,system:m.system,language:m.language,author:m.author,version:m.version,gameSha256:m.gameSha256,patchSha256,targetSha256:m.targetSha256,patchType:m.patchType,notes:m.notes,approved:false,objectKey,patchBytes:data.length,targetBytes:m.targetBytes||0,createdAt:new Date(now()).toISOString()};
    await store.set('translation/'+m.id,record);return json({translation:record},201);
   }
   if(path==='/api/translations/admin/approve' && req.method==='POST') {
    admin(req);const b=await body(req);if(!idOK(b.id) || typeof b.approved!=='boolean')fail(400,'Invalid approval');
    const m=await store.get('translation/'+b.id);if(!m)fail(404,'Not found');m.approved=b.approved;m.reviewedAt=new Date(now()).toISOString();await store.set('translation/'+b.id,m);return json({id:b.id,approved:b.approved});
   }
   if(path==='/api/translations/admin/enrollment' && req.method==='POST') {
    admin(req);const code=randomBytes(24).toString('hex');await store.set('enrollment/'+sha(code),{expires:now()+600000});return json({code,expiresIn:600});
   }
   if(path==='/api/translations/enroll' && req.method==='POST') {
    const b=await body(req);if(typeof b.code!=='string' || !/^[a-f0-9]{48}$/.test(b.code))fail(401,'Invalid enrollment');
    const key='enrollment/'+sha(b.code),record=await store.get(key);if(!record || record.expires<now())fail(401,'Enrollment unavailable');
    // A repeated/concurrent request derives the same device identity, never multiple grants.
    const token=sign('device:'+b.code);await store.set('device/'+sha(token),{expires:now()+86400000});await store.delete(key);return json({token,expiresIn:86400});
   }
   if(path==='/api/translations/admin/revoke' && req.method==='POST') {
    admin(req);const b=await body(req);if(!hashOK(b.deviceHash))fail(400,'Invalid device identity');await store.set('device/'+b.deviceHash,{revoked:true});return json({revoked:true});
   }
   if(path==='/api/translations/ticket' && req.method==='POST') {
    const d=await device(req),b=await body(req);if(!idOK(b.id))fail(400,'Invalid ID');const m=await store.get('translation/'+b.id);
    if(!m?.approved)fail(404,'Translation unavailable');if(b.gameSha256!==m.gameSha256)fail(409,'Incompatible game hash');
    const expires=now()+120000,signature=sign(`${m.id}:${m.patchSha256}:${d}:${expires}`);
    return json({url:`${u.origin}/api/translations/download/${m.id}?expires=${expires}&signature=${signature}`,expiresIn:120});
   }
   if(path.startsWith('/api/translations/download/') && req.method==='GET') {
    const d=await device(req),id=path.split('/').at(-1);if(!idOK(id))fail(404,'Not found');
    const m=await store.get('translation/'+id),expires=Number(u.searchParams.get('expires'));
    if(!m?.approved || !Number.isSafeInteger(expires) || expires<now() || expires>now()+120000 || !equal(u.searchParams.get('signature'),sign(`${id}:${m.patchSha256}:${d}:${expires}`)))fail(403,'Expired or invalid ticket');
    const bytes=await store.getBytes(m.objectKey);if(!bytes || sha(bytes)!==m.patchSha256)fail(503,'Patch integrity failure');
    return new Response(bytes,{headers:{...headers,'Content-Type':'application/octet-stream','Content-Disposition':`attachment; filename="${id}.${m.patchType.toLowerCase()}"`}});
   }
   return json({error:'Not found'},404);
  }catch(e){return json({error:e.status?e.message:'Service unavailable'},e.status||503);}
 };
}
