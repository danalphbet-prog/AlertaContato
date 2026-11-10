package com.diemdev.alertacontato;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int BG=0xff101722, CARD=0xff202c3b, WHITE=0xfff3f7fb, RED=0xffe73a4d;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private LinearLayout page, root, nav;
    private String screen="inicio";
    private boolean armed=false, countdown=false;
    private Runnable holdTask, alarmTask;
    private final SecureRandom random = new SecureRandom();
    private DatabaseReference observedRef;
    private ValueEventListener observedListener;
    private final List<DatabaseReference> liveRefs = new ArrayList<>();
    private final List<ValueEventListener> liveListeners = new ArrayList<>();
    private final Set<String> deliveredAlerts = new HashSet<>();
    private final Set<String> initializedContacts = new HashSet<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FirebaseOptions options = new FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_ANDROID_APP_ID)
            .setApiKey("AIzaSyB0kW5EMstX_O-UMyqLlDvHU9Iytd0x1kQ")
            .setProjectId("appqua-22")
            .setDatabaseUrl("https://appqua-22-default-rtdb.firebaseio.com")
            .setGcmSenderId("122122220091").build();
        if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options);
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        AlertNotifier.makeChannel(this);
        if (android.os.Build.VERSION.SDK_INT>=33 && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},44);
        if (auth.getCurrentUser()==null) showAuth(); else { show("inicio"); }
    }
    private DatabaseReference ref(String path){return database.getReference(path);}
    private String uid(){return auth.getUid();}
    private void toast(String text){ Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    private TextView text(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(WHITE);
        if(bold)t.setTypeface(null,1);t.setPadding(4,10,4,10);return t;
    }
    private LinearLayout vertical(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(22,15,22,15);return l; }
    private void base(String title, boolean withNav){
        root=vertical();root.setBackgroundColor(BG);root.setPadding(0,0,0,0);
        TextView top=text("  ◉  ALERTA CONTATO     /     "+title.toUpperCase(),19,true);top.setPadding(20,25,10,20);root.addView(top);
        ScrollView sc=new ScrollView(this);page=vertical();sc.setFillViewport(true);sc.addView(page);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        if(withNav){ nav=new LinearLayout(this);nav.setPadding(8,8,8,16);nav.setBackgroundColor(CARD);
            for(String s:new String[]{"Início","Contatos","Perfil"}){
                String dest=s.equals("Início")?"inicio":s.equals("Contatos")?"contatos":"perfil";
                Button b=button(s,()->show(dest));nav.addView(b,new LinearLayout.LayoutParams(0,56,1));
            }root.addView(nav);
        }
        setContentView(root);
    }
    private Button button(String label,Runnable click){ Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(CARD));b.setOnClickListener(v->click.run());return b; }
    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setText(value==null?"":value);e.setTextColor(WHITE);e.setHintTextColor(0xff9fabb9);page.addView(e,new LinearLayout.LayoutParams(-1,60));return e;}
    private void label(String title){page.addView(text(title,20,true));}
    private void showAuth(){
        base("Cadastro e login",false);label("Sua rede pessoal de segurança");
        EditText name=field("Nome completo (para cadastro)","");
        EditText email=field("E-mail","");
        EditText pass=field("Senha (mínimo 6 caracteres)","");pass.setInputType(129);
        page.addView(button("Entrar",()->{
            if(!valid(email,pass))return;
            auth.signInWithEmailAndPassword(email.getText().toString().trim(),pass.getText().toString())
                .addOnSuccessListener(r->{show("inicio");}).addOnFailureListener(e->toast("Falha no login: "+e.getMessage()));
        }));
        page.addView(button("Criar conta e ID pessoal",()->{
            if(!valid(email,pass))return;
            if(name.getText().toString().trim().length()<2){toast("Informe seu nome.");return;}
            auth.createUserWithEmailAndPassword(email.getText().toString().trim(),pass.getText().toString())
                .addOnSuccessListener(r->createProfile(name.getText().toString().trim(),email.getText().toString().trim()))
                .addOnFailureListener(e->toast("Erro no cadastro: "+e.getMessage()));
        }));
        page.addView(text("Use um e-mail válido. Sua senha fica protegida pelo Firebase Authentication.",13,false));
    }
    private boolean valid(EditText email,EditText pass){if(!email.getText().toString().contains("@")||pass.length()<6){toast("Digite e-mail válido e senha com 6 ou mais caracteres.");return false;}return true;}
    private void createProfile(String name,String email){
        // IDs públicos não expõem diretamente o UID. A transação evita IDs duplicados.
        String id="AC-"+String.format(java.util.Locale.US,"%08d",random.nextInt(100000000));
        ref("ids/"+id).runTransaction(new com.google.firebase.database.Transaction.Handler(){
            @Override public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData d){
                if(d.getValue()!=null)return com.google.firebase.database.Transaction.abort();
                d.setValue(uid());return com.google.firebase.database.Transaction.success(d);
            }
            @Override public void onComplete(DatabaseError err,boolean committed,DataSnapshot snapshot){
                if(err!=null){toast("Erro criando ID: "+err.getMessage());return;}
                if(!committed){createProfile(name,email);return;}
                Map<String,Object> p=new HashMap<>();p.put("personalId",id);p.put("name",name);p.put("email",email);
                p.put("age","");p.put("allergies","");p.put("health","");p.put("bloodType","");p.put("sex","");p.put("socialName","");p.put("children","");p.put("neighborhood","");p.put("phone","");
                ref("profiles/"+uid()).setValue(p).addOnSuccessListener(v->{show("inicio");})
                  .addOnFailureListener(e->toast("Erro salvando perfil: "+e.getMessage()));
            }
        });
    }
    private void detach(){
        if(observedRef!=null&&observedListener!=null)observedRef.removeEventListener(observedListener);
        observedRef=null;observedListener=null;
    }
    private void liveDetach(){
        for(int i=0;i<liveRefs.size();i++)liveRefs.get(i).removeEventListener(liveListeners.get(i));
        liveRefs.clear();liveListeners.clear();initializedContacts.clear();
    }
    private void watch(DatabaseReference r, ValueEventListener l){liveRefs.add(r);liveListeners.add(l);r.addValueEventListener(l);}
    private void startLiveAlerts(){
        liveDetach();
        if(uid()==null)return;
        watch(ref("contacts/"+uid()),new ValueEventListener(){
            @Override public void onDataChange(DataSnapshot data){
                Set<String> current=new HashSet<>();
                for(DataSnapshot c:data.getChildren())if(Boolean.TRUE.equals(c.getValue(Boolean.class)))current.add(c.getKey());
                if(!initializedContacts.equals(current)){
                    // Recria observadores apenas quando os pareamentos mudam.
                    for(int i=1;i<liveRefs.size();i++)liveRefs.get(i).removeEventListener(liveListeners.get(i));
                    while(liveRefs.size()>1){liveRefs.remove(liveRefs.size()-1);liveListeners.remove(liveListeners.size()-1);}
                    initializedContacts.clear();initializedContacts.addAll(current);
                    for(String friend:current)watchFriend(friend);
                }
            }
            @Override public void onCancelled(DatabaseError e){toast("Sem acesso aos contatos: "+e.getMessage());}
        });
    }
    private void watchFriend(String friend){
        DatabaseReference r=ref("status/"+friend);
        ValueEventListener listener=new ValueEventListener(){
            boolean first=true;
            @Override public void onDataChange(DataSnapshot snapshot){
                if(first){first=false;return;} // não toca alarmes antigos ao conectar
                if(!"SOS".equals(snapshot.child("state").getValue(String.class)))return;
                String event=snapshot.child("eventId").getValue(String.class);
                if(event==null||!deliveredAlerts.add(friend+"/"+event))return;
                AlertNotifier.notifySos(MainActivity.this,friend);
            }
            @Override public void onCancelled(DatabaseError error){}
        };
        watch(r,listener);
    }
    private void show(String target){
        detach();cancelTimers();screen=target;
        if(auth.getCurrentUser()==null){showAuth();return;}
        startLiveAlerts();
        base(target,true);
        switch(target){case "contatos":contacts();break;case "perfil":profile();break;default:home();}
    }
    private void cancelTimers(){if(holdTask!=null)handler.removeCallbacks(holdTask);if(alarmTask!=null)handler.removeCallbacks(alarmTask);armed=false;countdown=false;}
    private void home(){
        label("Proteção em um toque");
        page.addView(text("Segure o botão por 3 segundos para armar. Ao soltar, você terá mais 3 segundos para cancelar antes do disparo do SOS.",16,false));
        TextView status=text("PRONTO PARA ARMAR",18,true);status.setGravity(Gravity.CENTER);page.addView(status);
        Button sos=button("SEGURE 3 SEGUNDOS",()->{});
        sos.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED));sos.setTextSize(19);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,155);lp.setMargins(0,20,0,15);page.addView(sos,lp);
        Button cancel=button("CANCELAR ALERTA",()->{cancelTimers();status.setText("CANCELADO — nenhum SOS enviado");});page.addView(cancel);
        sos.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                cancelTimers();status.setText("SEGURANDO... mantenha por 3 segundos");
                holdTask=()->{armed=true;status.setText("ARMADO! Solte para iniciar contagem");};handler.postDelayed(holdTask,3000);return true;
            }
            if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
                if(holdTask!=null)handler.removeCallbacks(holdTask);
                if(armed){armed=false;countdown=true;status.setText("3... toque CANCELAR para impedir o SOS");
                    handler.postDelayed(()->{if(countdown)status.setText("2... CANCELAR");},1000);
                    handler.postDelayed(()->{if(countdown)status.setText("1... CANCELAR");},2000);
                    alarmTask=()->{if(!countdown)return;countdown=false;status.setText("ENVIANDO SOS...");sendAlert(status);};handler.postDelayed(alarmTask,3000);
                }else { status.setText("Não armado: segure por 3 segundos completos"); }
                return true;
            }return true;
        });
        page.addView(text("IMPORTANTE: Notificações podem atrasar por falta de rede, bateria, permissões ou ajustes do Android. Não substitui ligação para 190 / 192 / 193.",14,false));
    }
    private void sendAlert(TextView status){
        if(uid()==null)return;
        ref("contacts/"+uid()).get().addOnSuccessListener(snapshot->{
            if(!snapshot.exists()){status.setText("Nenhum contato pareado. SOS não enviado.");return;}
            String key=ref("status/"+uid()).push().getKey();
            Map<String,Object> a=new HashMap<>();a.put("state","SOS");a.put("createdAt",ServerValue.TIMESTAMP);a.put("eventId",key);
            ref("status/"+uid()).setValue(a).addOnSuccessListener(v->status.setText("SOS publicado em tempo real para contatos conectados."))
                .addOnFailureListener(e->status.setText("FALHA AO ENVIAR: "+e.getMessage()));
        }).addOnFailureListener(e->status.setText("Falha consultando contatos: "+e.getMessage()));
    }
    private void contacts(){
        label("Adicionar por ID pessoal");
        EditText id=field("Ex.: AC-12345678","");
        page.addView(button("Enviar solicitação",()->{
            String code=id.getText().toString().trim().toUpperCase(java.util.Locale.ROOT);
            if(!code.matches("AC-[0-9]{8}")){toast("ID inválido.");return;}
            ref("ids/"+code).get().addOnSuccessListener(s->{
                String dest=s.getValue(String.class);if(dest==null){toast("ID não localizado.");return;}
                if(dest.equals(uid())){toast("Você não pode adicionar a si mesmo.");return;}
                Map<String,Object> req=new HashMap<>();req.put("status","pending");req.put("createdAt",ServerValue.TIMESTAMP);
                ref("requests/"+dest+"/"+uid()).setValue(req).addOnSuccessListener(v->toast("Solicitação enviada!"))
                    .addOnFailureListener(e->toast("Erro: "+e.getMessage()));
            }).addOnFailureListener(e->toast("Falha na busca: "+e.getMessage()));
        }));
        label("Solicitações recebidas");
        LinearLayout requests=vertical();page.addView(requests);
        label("Contatos pareados");
        LinearLayout matched=vertical();page.addView(matched);
        observedRef=ref("requests/"+uid());observedListener=new ValueEventListener(){
            @Override public void onDataChange(DataSnapshot snap){
                requests.removeAllViews();int count=0;
                for(DataSnapshot child:snap.getChildren()){
                    if(!"pending".equals(child.child("status").getValue(String.class)))continue;
                    count++;String sender=child.getKey();
                    TextView name=text("Pedido de: "+sender,15,true);requests.addView(name);
                    ref("profiles/"+sender+"/name").get().addOnSuccessListener(s->{if(s.getValue()!=null)name.setText("Pedido de: "+s.getValue(String.class));});
                    requests.addView(button("Aceitar",()->ref("requests/"+uid()+"/"+sender+"/status").setValue("accepted")
                        .addOnSuccessListener(v->{
                            Map<String,Object> links=new HashMap<>();
                            links.put("contacts/"+uid()+"/"+sender,true);
                            links.put("contacts/"+sender+"/"+uid(),true);
                            database.getReference().updateChildren(links)
                                .addOnSuccessListener(ok->toast("Contato pareado com sucesso!"))
                                .addOnFailureListener(e->toast("Pedido aceito, mas o vínculo falhou: "+e.getMessage()));
                        })
                        .addOnFailureListener(e->toast("Falha: "+e.getMessage()))));
                    requests.addView(button("Recusar",()->ref("requests/"+uid()+"/"+sender+"/status").setValue("rejected")));
                }if(count==0)requests.addView(text("Nenhuma solicitação pendente.",14,false));
            }
            @Override public void onCancelled(DatabaseError e){toast("Erro nos pedidos: "+e.getMessage());}
        };observedRef.addValueEventListener(observedListener);
        ref("contacts/"+uid()).addValueEventListener(new ValueEventListener(){
            @Override public void onDataChange(DataSnapshot s){
                if(!"contatos".equals(screen))return;
                matched.removeAllViews();if(!s.exists()){matched.addView(text("Nenhum contato pareado ainda.",14,false));return;}
                for(DataSnapshot item:s.getChildren()){
                    String friend=item.getKey();TextView row=text("Contato: "+friend,16,true);matched.addView(row);
                    ref("profiles/"+friend+"/name").get().addOnSuccessListener(n->{if(n.getValue()!=null)row.setText("✓ "+n.getValue(String.class));});
                }
            }
            @Override public void onCancelled(DatabaseError e){}
        });
    }
    private void profile(){
        label("Seu perfil de emergência");
        ref("profiles/"+uid()).get().addOnSuccessListener(s->{
            if(!"perfil".equals(screen))return;
            String personalId=s.child("personalId").getValue(String.class);
            page.addView(text("SEU ID: "+(personalId==null?"Aguardando cadastro":personalId),21,true));
            String[] fields={"name","socialName","age","bloodType","sex","children","allergies","health","neighborhood","phone","email"};
            String[] titles={"Nome","Nome social","Idade","Tipo sanguíneo","Sexo","Tem filhos?","Alergias","Informações médicas","Bairro/cidade","Telefone","E-mail de contato"};
            Map<String,EditText> edits=new HashMap<>();
            for(int i=0;i<fields.length;i++){EditText e=field(titles[i],s.child(fields[i]).getValue(String.class));edits.put(fields[i],e);}
            page.addView(button("Salvar perfil",()->{
                Map<String,Object> values=new HashMap<>();for(String key:fields)values.put(key,edits.get(key).getText().toString().trim());
                ref("profiles/"+uid()).updateChildren(values).addOnSuccessListener(v->toast("Perfil salvo!"))
                    .addOnFailureListener(e->toast("Erro: "+e.getMessage()));
            }));
            page.addView(text("Seus dados de saúde ficam disponíveis para contatos pareados. Preencha apenas o que desejar compartilhar.",13,false));
            page.addView(button("Sair da conta",()->{liveDetach();auth.signOut();showAuth();}));
        }).addOnFailureListener(e->toast("Erro carregando perfil: "+e.getMessage()));
    }
    @Override protected void onDestroy(){detach();liveDetach();cancelTimers();super.onDestroy();}
}
