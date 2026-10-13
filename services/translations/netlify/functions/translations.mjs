import {getStore} from '@netlify/blobs';
import {createService} from '../../server/service.mjs';
export default async request=>{
 const blobs=getStore({name:'bruma-translations',consistency:'strong'});
 const store={get:k=>blobs.get(k,{type:'json'}),set:(k,v)=>blobs.setJSON(k,v),getBytes:async k=>{const v=await blobs.get(k,{type:'arrayBuffer'});return v?new Uint8Array(v):null;},setBytes:(k,v)=>blobs.set(k,v),delete:k=>blobs.delete(k),async list(prefix){const result=[];for await(const page of blobs.list({prefix,paginate:true})){for(const b of page.blobs){const v=await this.get(b.key);if(v)result.push(v);}}return result;}};
 return createService(store,process.env)(request);
};
// Netlify's edge limiter is shared across instances; no local-memory counter.
// Free plans support code-based per-IP rules. Verify the rule in deploy logs.
export const config={path:'/api/translations/*',rateLimit:{windowLimit:60,windowSize:60,aggregateBy:['ip','domain']}};
