package com.mishwar.app;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SupabaseClient {
    public interface Callback { void done(boolean ok, String body, String error); }
    private final String base;
    private final String key;
    private final android.content.SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());

    public SupabaseClient(android.content.Context c) {
        base = "https://epfudznfazcqrpxqkzfx.supabase.co";
        key = "sb_publishable_k5zdp1T_OkI72NLjXXfaSw_LLsMm49f";
        prefs = c.getSharedPreferences("mishwar_supabase", 0);
    }

    public void ensureAnonymous(Callback cb) {
        String access = prefs.getString("access_token", null);
        String refresh = prefs.getString("refresh_token", null);
        if (access != null && !access.isEmpty()) { cb.done(true, "cached", null); return; }
        if (refresh != null && !refresh.isEmpty()) {
            request("/auth/v1/token?grant_type=refresh_token", "POST", "{\"refresh_token\":" + JSONObject.quote(refresh) + "}", null, new Callback() {
                public void done(boolean ok, String body, String err) {
                    if (ok) { saveSession(body); cb.done(true, body, null); }
                    else anonymousSignup(cb);
                }
            });
        } else anonymousSignup(cb);
    }

    private void anonymousSignup(Callback cb) {
        request("/auth/v1/signup", "POST", "{}", null, new Callback() {
            public void done(boolean ok, String body, String err) {
                if (ok) { saveSession(body); prefs.edit().putBoolean("driver_session", false).apply(); cb.done(true, body, null); }
                else cb.done(false, body, err);
            }
        });
    }

    private void saveSession(String body) {
        try {
            JSONObject j = new JSONObject(body);
            String a=j.optString("access_token",""); String r=j.optString("refresh_token","");
            if (!a.isEmpty()) prefs.edit().putString("access_token",a).putString("refresh_token",r).apply();
        } catch(Exception ignored) {}
    }

    public String userIdFromSession() {
        try {
            String access=prefs.getString("access_token","");
            if (access.isEmpty()) return "";
            String[] p=access.split("\\.");
            if(p.length<2)return "";
            String payload = new String(
    android.util.Base64.decode(
        p[1],
        android.util.Base64.URL_SAFE
            | android.util.Base64.NO_WRAP
            | android.util.Base64.NO_PADDING
    ),
    java.nio.charset.StandardCharsets.UTF_8
    );
            return new JSONObject(payload).optString("sub","");
        } catch(Exception e){return "";}
    }

    public void upsertAppUser(Callback cb) {
        String uid=userIdFromSession(); if(uid.isEmpty()){cb.done(false,"","No user id");return;}
        JSONObject row=new JSONObject(); try {row.put("user_id",uid); row.put("last_seen",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).format(new Date())); row.put("first_seen",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).format(new Date()));}catch(Exception ignored){}
        request("/rest/v1/app_users?on_conflict=user_id", "POST", "["+row.toString()+"]", "Prefer: resolution=merge-duplicates,return=minimal", cb);
    }

    public void event(String type, String tripId) {
        String uid=userIdFromSession(); if(uid.isEmpty())return;
        try {
            JSONObject row=new JSONObject(); row.put("user_id",uid); row.put("event_type",type); if(tripId!=null&&!tripId.isEmpty())row.put("trip_id",tripId); row.put("metadata",new JSONObject());
            request("/rest/v1/analytics_events", "POST", "["+row.toString()+"]", "Prefer: return=minimal", new Callback(){public void done(boolean ok,String body,String error){}});
        } catch(Exception ignored){}
    }

    public void loadTrips(Callback cb) {
        String q="/rest/v1/trips?select=id,destination,seats,seats_available,notes,status,drivers(name,phone,whatsapp,vehicle_type)&status=eq.available&order=created_at.desc";
        request(q,"GET",null,null,cb);
    }


    public void signUpDriver(String email, String password, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            request("/auth/v1/signup", "POST", body.toString(), null, new Callback(){
                public void done(boolean ok, String response, String err) {
                    if(ok) { saveSession(response); prefs.edit().putBoolean("driver_session", true).apply(); cb.done(true,response,null); }
                    else cb.done(false,response,err);
                }
            });
        } catch(Exception e){ cb.done(false,"",e.toString()); }
    }

    public void signInDriver(String email, String password, Callback cb) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);
            request("/auth/v1/token?grant_type=password", "POST", body.toString(), null, new Callback(){
                public void done(boolean ok, String response, String err) {
                    if(ok) { saveSession(response); prefs.edit().putBoolean("driver_session", true).apply(); cb.done(true,response,null); }
                    else cb.done(false,response,err);
                }
            });
        } catch(Exception e){ cb.done(false,"",e.toString()); }
    }

    public void signOut(){ prefs.edit().remove("access_token").remove("refresh_token").putBoolean("driver_session", false).apply(); }

    public boolean hasDriverSession(){ return prefs.getBoolean("driver_session", false) && !userIdFromSession().isEmpty(); }

    public void saveDriver(String name, String phone, String whatsapp, String vehicle, Callback cb) {
        String uid=userIdFromSession();
        if(uid.isEmpty()){cb.done(false,"","No authenticated user");return;}
        try {
            JSONObject row=new JSONObject(); row.put("id",uid); row.put("name",name); row.put("phone",phone); row.put("whatsapp",whatsapp); row.put("vehicle_type",vehicle); row.put("is_active",true);
            request("/rest/v1/drivers?on_conflict=id","POST","["+row.toString()+"]","Prefer: resolution=merge-duplicates,return=representation",cb);
        } catch(Exception e){cb.done(false,"",e.toString());}
    }


    public void loadMyDriver(Callback cb) {
        String uid=userIdFromSession();
        if(uid.isEmpty()){cb.done(false,"","No authenticated user");return;}
        request("/rest/v1/drivers?select=id,name,phone,whatsapp,vehicle_type,is_active&id=eq."+uid,"GET",null,null,cb);
    }

    public void updateMyDriver(String name,String phone,String whatsapp,String vehicle,boolean active,Callback cb) {
        String uid=userIdFromSession();
        if(uid.isEmpty()){cb.done(false,"","No authenticated user");return;}
        try {
            JSONObject row=new JSONObject(); row.put("name",name); row.put("phone",phone); row.put("whatsapp",whatsapp); row.put("vehicle_type",vehicle); row.put("is_active",active);
            request("/rest/v1/drivers?id=eq."+URLEncoder.encode(uid,"UTF-8"),"PATCH",row.toString(),"Prefer: return=representation",cb);
        } catch(Exception e){cb.done(false,"",e.toString());}
    }

    public void loadMyTrips(Callback cb) {
        String uid=userIdFromSession();
        if(uid.isEmpty()){cb.done(false,"","No authenticated user");return;}
        request("/rest/v1/trips?select=id,destination,seats,seats_available,notes,status,created_at&driver_id=eq."+uid+"&order=created_at.desc","GET",null,null,cb);
    }

    public void createTrip(String destination,int seats,String notes,Callback cb) {
        String uid=userIdFromSession(); if(uid.isEmpty()){cb.done(false,"","No authenticated user");return;}
        try {
            JSONObject row=new JSONObject(); row.put("driver_id",uid); row.put("destination",destination); row.put("seats",seats); row.put("seats_available",seats); row.put("notes",notes==null?"":notes); row.put("status","available");
            request("/rest/v1/trips","POST","["+row.toString()+"]","Prefer: return=representation",cb);
        }catch(Exception e){cb.done(false,"",e.toString());}
    }

    public void updateTripSeats(String tripId, int seatsAvailable, String status, Callback cb) {
        if (tripId == null || tripId.isEmpty()) { cb.done(false,"","Missing trip id"); return; }
        try {
            JSONObject row = new JSONObject();
            row.put("seats_available", Math.max(0, seatsAvailable));
            row.put("status", status);
            request("/rest/v1/trips?id=eq." + URLEncoder.encode(tripId, "UTF-8"), "PATCH", row.toString(), "Prefer: return=representation", cb);
        } catch(Exception e){ cb.done(false,"",e.toString()); }
    }

    public void deleteTrip(String tripId, Callback cb) {
        if (tripId == null || tripId.isEmpty()) { cb.done(false,"","Missing trip id"); return; }
        try {
            request("/rest/v1/trips?id=eq." + URLEncoder.encode(tripId, "UTF-8"), "DELETE", null, "Prefer: return=minimal", cb);
        } catch(Exception e){ cb.done(false,"",e.toString()); }
    }

    private void request(String path,String method,String body,String extra,Callback cb) {
        new Thread(() -> {
            HttpURLConnection c=null;
            try {
                URL u=new URL(base+path); c=(HttpURLConnection)u.openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestProperty("apikey",key); c.setRequestProperty("Accept","application/json");
                String access=prefs.getString("access_token",""); if(!access.isEmpty())c.setRequestProperty("Authorization","Bearer "+access);
                if(extra!=null){String[] x=extra.split(": ",2); if(x.length==2)c.setRequestProperty(x[0],x[1]); else c.setRequestProperty("Prefer",extra);}
                if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream os=c.getOutputStream()){os.write(body.getBytes(StandardCharsets.UTF_8));}}
                int code=c.getResponseCode(); InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String out=read(is); final boolean ok=code>=200&&code<300;
                main.post(() -> cb.done(ok,out,ok?null:("HTTP "+code+": "+out)));
            } catch(Exception e){main.post(() -> cb.done(false,"",e.toString()));} finally {if(c!=null)c.disconnect();}
        }).start();
    }
    private static String read(InputStream is)throws Exception{if(is==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))!=-1)b.write(buf,0,n);return b.toString("UTF-8");}
}
