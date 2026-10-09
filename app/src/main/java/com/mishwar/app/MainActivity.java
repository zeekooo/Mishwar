package com.mishwar.app;

import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.os.Bundle;import android.text.InputType;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;

public class MainActivity extends Activity {
 final int GREEN=Color.rgb(21,148,71),DARK=Color.rgb(18,60,74),BG=Color.rgb(244,248,247); LinearLayout root,content; EditText search; SupabaseClient supa; ArrayList<Trip> trips=new ArrayList<>();
 Button clientTab, driverTab;

void setActiveTab(boolean client) {
    if (clientTab != null) {
        clientTab.setBackgroundColor(client ? GREEN : Color.WHITE);
        clientTab.setTextColor(client ? Color.WHITE : DARK);
    }
    if (driverTab != null) {
        driverTab.setBackgroundColor(client ? Color.WHITE : GREEN);
        driverTab.setTextColor(client ? DARK : Color.WHITE);
    }
}
 int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);} TextView tv(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(dp(4),dp(4),dp(4),dp(4));return t;} Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setTextColor(c==GREEN?Color.WHITE:DARK);b.setAllCaps(false);b.setMinHeight(dp(48));b.setBackgroundColor(c==GREEN?GREEN:Color.WHITE);return b;} EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackgroundColor(Color.WHITE);e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(52)));return e;} LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(12));c.setLayoutParams(p);return c;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);supa=new SupabaseClient(this);buildShell();showClient();supa.ensureAnonymous(new SupabaseClient.Callback(){public void done(boolean ok,String body,String err){if(ok){supa.upsertAppUser(new SupabaseClient.Callback(){public void done(boolean x,String y,String z){}});supa.event("app_open",null);loadTrips();}else toast("تعذر الاتصال بالخادم: "+err);}});}
 void buildShell() {
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(BG);
    root.setPadding(dp(14), dp(14), dp(14), dp(14));

    LinearLayout h = new LinearLayout(this);
    h.setOrientation(LinearLayout.VERTICAL);
    h.setPadding(dp(18), dp(14), dp(18), dp(14));
    h.setBackgroundColor(DARK);
    h.addView(tv("مشوار", 28, Color.WHITE));
    h.addView(tv("رحلتك... مع ناس على نفس الطريق", 13, Color.LTGRAY));
    root.addView(h);

    LinearLayout tabs = new LinearLayout(this);
    tabs.setPadding(0, dp(12), 0, dp(4));

    clientTab = btn("أنا عميل", GREEN);
    driverTab = btn("أنا سائق", Color.WHITE);

    tabs.addView(clientTab,
            new LinearLayout.LayoutParams(0, dp(52), 1));
    tabs.addView(driverTab,
            new LinearLayout.LayoutParams(0, dp(52), 1));

    root.addView(tabs);

    content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);

    ScrollView scroll = new ScrollView(this);
    scroll.addView(content);

    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    setContentView(root);

    clientTab.setOnClickListener(v -> showClient());
    driverTab.setOnClickListener(v -> showDriver());
}
 void showClient(){setActiveTab(true);
  content.removeAllViews();LinearLayout c=card();c.addView(tv("إلى أين تريد الذهاب؟",22,DARK));c.addView(tv("ابحث عن الرحلات المتاحة من قاعدة البيانات، ثم تواصل مباشرة مع السائق.",14,Color.DKGRAY));search=input("مثال: تعز");c.addView(search);Button go=btn("بحث",GREEN);c.addView(go);content.addView(c);go.setOnClickListener(v->{supa.event("search",null);renderResults(search.getText().toString().trim());});renderResults("");}
 void loadTrips(){supa.loadTrips(new SupabaseClient.Callback(){public void done(boolean ok,String body,String err){if(!ok){toast("لم يتم تحميل الرحلات: "+err);return;}try{trips.clear();JSONArray a=new JSONArray(body);for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i),d=j.optJSONObject("drivers");Trip t=new Trip(j.optString("id"),d==null?"سائق":"سائق: "+d.optString("name"),d==null?"":d.optString("vehicle_type"),j.optString("destination"),j.optInt("seats"),j.optInt("seats_available"),d==null?"":d.optString("phone"),d==null?"":d.optString("whatsapp"),j.optString("notes"),"available".equals(j.optString("status")));trips.add(t);}renderResults(search==null?"":search.getText().toString().trim());}catch(Exception e){toast("بيانات الرحلات غير مفهومة");}}});}
 void renderResults(String q){while(content.getChildCount()>1)content.removeViewAt(1);String s=q.toLowerCase(Locale.ROOT);if(trips.isEmpty()){content.addView(tv("لا توجد رحلات محملة حاليًا. إذا كان هناك سائق نشر رحلة ستظهر هنا.",14,Color.GRAY));return;}for(Trip t:trips){if(!s.isEmpty()&&!t.dest.toLowerCase(Locale.ROOT).contains(s))continue;LinearLayout c=card();LinearLayout top=new LinearLayout(this);top.addView(tv(t.car.isEmpty()?"مركبة":t.car,18,DARK),new LinearLayout.LayoutParams(0,-2,1));top.addView(tv(t.available?"● متوفر":"● مكتمل",13,t.available?GREEN:Color.RED));c.addView(top);c.addView(tv("📍 إلى "+t.dest,15,DARK));c.addView(tv("👤 "+t.name+"   •   💺 "+t.seats+"   •   المتبقي: "+t.left,13,Color.DKGRAY));if(!t.notes.isEmpty())c.addView(tv("📝 "+t.notes,12,Color.GRAY));LinearLayout acts=new LinearLayout(this);Button call=btn("📞 اتصال",Color.WHITE),wa=btn("💬 واتساب",GREEN);acts.addView(call,new LinearLayout.LayoutParams(0,dp(50),1));acts.addView(wa,new LinearLayout.LayoutParams(0,dp(50),1));c.addView(acts);call.setOnClickListener(v->{supa.event("call_click",t.id);dial(t.phone);});wa.setOnClickListener(v->{supa.event("whatsapp_click",t.id);whatsapp(t.wa);});content.addView(c);}}
 void showDriver(){setActiveTab(false);
   if(supa.hasDriverSession()){showDriverHome();return;}
   content.removeAllViews();LinearLayout c=card();c.addView(tv("حساب السائق",22,DARK));
   EditText email=input("البريد الإلكتروني للسائق");EditText pass=input("كلمة المرور (6 أحرف أو أكثر)");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);EditText name=input("الاسم الكامل");EditText phone=input("رقم الهاتف");EditText wa=input("رقم واتساب");EditText vehicle=input("نوع المركبة");c.addView(email);c.addView(pass);c.addView(name);c.addView(phone);c.addView(wa);c.addView(vehicle);
   LinearLayout actions=new LinearLayout(this);Button signup=btn("إنشاء حساب",GREEN),login=btn("تسجيل الدخول",Color.WHITE);actions.addView(signup,new LinearLayout.LayoutParams(0,dp(52),1));actions.addView(login,new LinearLayout.LayoutParams(0,dp(52),1));c.addView(actions);content.addView(c);
   signup.setOnClickListener(v->{if (email.getText().toString().trim().isEmpty()
        || pass.getText().toString().length() < 6
        || name.getText().toString().trim().isEmpty()
        || phone.getText().toString().trim().isEmpty()
        || wa.getText().toString().trim().isEmpty()
        || vehicle.getText().toString().trim().isEmpty()) {
    toast("يرجى تعبئة جميع الحقول، وكلمة المرور يجب أن تكون 6 أحرف على الأقل");
    return;
}supa.signUpDriver(email.getText().toString().trim(),pass.getText().toString(),(ok,body,err)->{if(!ok){toast("فشل إنشاء الحساب: "+err);return;}supa.saveDriver(name.getText().toString().trim(),phone.getText().toString().trim(),wa.getText().toString().trim(),vehicle.getText().toString().trim(),(x,y,z)->{if(!x){toast("تم إنشاء الحساب لكن تعذر حفظ بيانات السائق: "+z);return;}toast("تم إنشاء حساب السائق بنجاح");showDriverHome();});});});
   login.setOnClickListener(v->{supa.signInDriver(email.getText().toString().trim(),pass.getText().toString(),(ok,body,err)->{if(!ok){toast("فشل تسجيل الدخول: "+err);return;}showDriverHome();});});
 }
 void showDriverHome(){content.removeAllViews();
   LinearLayout head=card();head.addView(tv("لوحة السائق",23,DARK));head.addView(tv("إدارة الحساب والرحلات من مكان واحد.",14,Color.DKGRAY));
   Button profile=btn("👤 حسابي وتعديل بياناتي",Color.WHITE), tripsBtn=btn("🚗 رحلاتي",Color.WHITE), logout=btn("تسجيل الخروج",Color.WHITE);head.addView(profile);head.addView(tripsBtn);head.addView(logout);content.addView(head);
   profile.setOnClickListener(v->showDriverProfile());tripsBtn.setOnClickListener(v->loadDriverTrips());logout.setOnClickListener(v->{supa.signOut();toast("تم تسجيل الخروج");showDriver();});
   LinearLayout c=card();c.addView(tv("نشر رحلة جديدة",20,DARK));EditText dest=input("الوجهة: مثال تعز"),seats=input("عدد المقاعد"),notes=input("ملاحظات اختيارية");c.addView(dest);c.addView(seats);c.addView(notes);Button publish=btn("نشر الرحلة",GREEN);c.addView(publish);content.addView(c);
   publish.setOnClickListener(v->{int n;try{n=Integer.parseInt(seats.getText().toString().trim());}catch(Exception e){n=0;}if(dest.getText().toString().trim().isEmpty()||n<1){toast("أدخل الوجهة وعدد المقاعد");return;}supa.createTrip(dest.getText().toString().trim(),n,notes.getText().toString().trim(),(ok,body,err)->{if(ok){toast("تم نشر الرحلة");supa.event("trip_created",null);loadTrips();loadDriverTrips();}else toast("تعذر نشر الرحلة: "+err);});});
 }
 void showDriverProfile(){content.removeAllViews();LinearLayout c=card();c.addView(tv("حسابي",23,DARK));c.addView(tv("يمكنك تعديل بيانات التواصل والمركبة دون إنشاء حساب جديد.",14,Color.DKGRAY));EditText name=input("الاسم الكامل"),phone=input("رقم الهاتف"),wa=input("رقم واتساب"),vehicle=input("نوع المركبة");c.addView(name);c.addView(phone);c.addView(wa);c.addView(vehicle);Button save=btn("حفظ التعديلات",GREEN),back=btn("العودة للوحة السائق",Color.WHITE);c.addView(save);c.addView(back);content.addView(c);
   supa.loadMyDriver((ok,body,err)->{if(!ok){toast("تعذر تحميل بيانات الحساب: "+err);return;}try{JSONArray a=new JSONArray(body);if(a.length()==0){toast("لم يتم العثور على حساب السائق");return;}JSONObject j=a.getJSONObject(0);name.setText(j.optString("name"));phone.setText(j.optString("phone"));wa.setText(j.optString("whatsapp"));vehicle.setText(j.optString("vehicle_type"));}catch(Exception e){toast("بيانات الحساب غير مفهومة");}});
   save.setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()){toast("الاسم مطلوب");return;}supa.updateMyDriver(name.getText().toString().trim(),phone.getText().toString().trim(),wa.getText().toString().trim(),vehicle.getText().toString().trim(),true,(ok,body,err)->{if(ok){toast("تم حفظ التعديلات");showDriverHome();}else toast("تعذر حفظ التعديلات: "+err);});});back.setOnClickListener(v->showDriverHome());
 }
 void loadDriverTrips(){supa.loadMyTrips((ok,body,err)->{if(!ok){toast("تعذر تحميل رحلاتك: "+err);return;}try{JSONArray a=new JSONArray(body);content.removeAllViews();LinearLayout head=card();head.addView(tv("رحلاتي",23,DARK));Button back=btn("العودة للوحة السائق",Color.WHITE);head.addView(back);content.addView(head);back.setOnClickListener(v->showDriverHome());if(a.length()==0){content.addView(tv("لا توجد رحلات منشورة بعد.",14,Color.GRAY));return;}for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);String tripId=j.optString("id");int total=j.optInt("seats"),left=j.optInt("seats_available");String status=j.optString("status");LinearLayout c=card();c.addView(tv("📍 "+j.optString("destination"),18,DARK));c.addView(tv("💺 المتاح: "+left+" من "+total+" مقعد",14,Color.DKGRAY));c.addView(tv("الحالة: "+("available".equals(status)?"متاحة":"full".equals(status)?"مكتملة":"ملغاة"),13,"available".equals(status)?GREEN:Color.RED));LinearLayout controls=new LinearLayout(this);Button minus=btn("− مقعد",Color.WHITE),plus=btn("+ مقعد",Color.WHITE),full=btn("إكمال الرحلة",GREEN);controls.addView(minus,new LinearLayout.LayoutParams(0,dp(50),1));controls.addView(plus,new LinearLayout.LayoutParams(0,dp(50),1));controls.addView(full,new LinearLayout.LayoutParams(0,dp(50),1));c.addView(controls);Button del=btn("حذف الرحلة",Color.WHITE);c.addView(del);minus.setEnabled(left>0&&"available".equals(status));plus.setEnabled(left<total&&"available".equals(status));full.setEnabled("available".equals(status));minus.setOnClickListener(v->{int n=Math.max(0,left-1);supa.updateTripSeats(tripId,n,n==0?"full":"available",(x,y,z)->{if(x){toast(n==0?"اكتملت المقاعد":"تم تحديث المقاعد");loadDriverTrips();loadTrips();}else toast("تعذر التحديث: "+z);});});plus.setOnClickListener(v->{int n=Math.min(total,left+1);supa.updateTripSeats(tripId,n,"available",(x,y,z)->{if(x){toast("تم تحديث المقاعد");loadDriverTrips();loadTrips();}else toast("تعذر التحديث: "+z);});});full.setOnClickListener(v->supa.updateTripSeats(tripId,0,"full",(x,y,z)->{if(x){toast("تم وضع الرحلة كمكتملة");loadDriverTrips();loadTrips();}else toast("تعذر التحديث: "+z);}));del.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle("حذف الرحلة؟").setMessage("سيتم حذف الرحلة من قاعدة البيانات.").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(d,w)->supa.deleteTrip(tripId,(x,y,z)->{if(x){toast("تم حذف الرحلة");loadDriverTrips();loadTrips();}else toast("تعذر الحذف: "+z);})).show();});content.addView(c);}}catch(Exception e){toast("تعذر قراءة رحلاتك");}});}
 void showAdmin(){content.removeAllViews();LinearLayout c=card();c.addView(tv("لوحة الإحصائيات",23,DARK));c.addView(tv("الإحصائيات تُرسل إلى Supabase من التطبيق. لوحة الإدارة المنفصلة سنبنيها لاحقًا.",14,Color.DKGRAY));c.addView(tv("معرّف المستخدم الحالي: "+supa.userIdFromSession(),12,Color.GRAY));content.addView(c);}
 void dial(String p){if(p==null||p.isEmpty()){toast("لا يوجد رقم");return;}startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+p)));} void whatsapp(String p){if(p==null||p.isEmpty()){toast("لا يوجد رقم واتساب");return;}String n=p.replaceAll("\\D","");if(n.startsWith("0"))n="967"+n.substring(1);startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+n)));} void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 static class Trip{String id,name,car,dest,phone,wa,notes;int seats,left;boolean available;Trip(String i,String n,String c,String d,int s,int l,String p,String w,String no,boolean a){id=i;name=n;car=c;dest=d;seats=s;left=l;phone=p;wa=w;notes=no;available=a;}}
}
