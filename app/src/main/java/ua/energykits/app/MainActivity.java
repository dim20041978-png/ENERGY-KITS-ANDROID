package ua.energykits.app;
import android.app.*; import android.os.*; import android.widget.*; import android.view.*; import android.graphics.Color; import android.content.*; import android.net.Uri; import org.json.*; import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root, results; EditText inv,bat,ip,bp,iv,bv,ah,bms,watts; JSONArray kits=new JSONArray();
 EditText field(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);root.addView(e);return e;}
 void button(String name,Runnable action){Button b=new Button(this);b.setText(name);root.addView(b);b.setOnClickListener(v->action.run());}
 double num(EditText e){double x=Double.parseDouble(e.getText().toString().replace(',','.'));if(!Double.isFinite(x)||x<=0)throw new IllegalArgumentException();return x;}
 public void onCreate(Bundle b){super.onCreate(b);try{kits=new JSONArray(getPreferences(0).getString("kits","[]"));}catch(Exception ignored){}
 ScrollView s=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,48,24,32);root.setBackgroundColor(Color.rgb(12,32,60));s.addView(root);setContentView(s);
 TextView title=new TextView(this);title.setText("⚡ Підбір енергокомплектів");title.setTextSize(25);root.addView(title);
 inv=field("Модель інвертора",1);ip=field("Ціна інвертора, грн",8194);iv=field("Номінальна напруга системи, В",8194);watts=field("Потужність інвертора, Вт",8194);
 button("Знайти інвертор в інтернеті",()->search(inv.getText().toString()+" інвертор ціна Україна"));
 bat=field("Модель акумулятора",1);bp=field("Ціна АКБ, грн",8194);bv=field("Номінальна напруга АКБ, В",8194);ah=field("Ємність АКБ, А·год",8194);bms=field("Тривалий струм BMS, А",8194);
 button("Знайти акумулятор в інтернеті",()->search(bat.getText().toString()+" LiFePO4 акумулятор ціна Україна"));
 button("Додати комплект",()->add());button("Новий вибір",()->{for(EditText e:new EditText[]{inv,bat,ip,bp,iv,bv,ah,bms,watts})e.setText("");});
 results=new LinearLayout(this);results.setOrientation(1);root.addView(results);render();}
 void search(String q){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))));}catch(Exception e){Toast.makeText(this,"Браузер недоступний",0).show();}}
 void add(){try{double v=num(iv),a=num(bv),p=num(watts),current=num(bms);if(inv.getText().toString().trim().isEmpty()||bat.getText().toString().trim().isEmpty())throw new IllegalArgumentException();JSONObject k=new JSONObject();k.put("name",inv.getText()+" + "+bat.getText());k.put("price",num(ip)+num(bp));k.put("energy",a*num(ah)/1000);k.put("note",Math.abs(v-a)/v>0.1?"Напруга не відповідає":p>v*0.9*current*0.9?"BMS може обмежувати потужність":"Попередня оцінка струму достатня; перевірте паспорт і протоколи");kits.put(k);save();render();}catch(Exception e){Toast.makeText(this,"Заповніть усі поля коректними значеннями",0).show();}}
 void save(){getPreferences(0).edit().putString("kits",kits.toString()).apply();}
 void render(){results.removeAllViews();ArrayList<JSONObject> list=new ArrayList<>();for(int i=0;i<kits.length();i++)list.add(kits.optJSONObject(i));list.sort(Comparator.comparingDouble(k->k.optDouble("price")));for(JSONObject k:list){TextView t=new TextView(this);t.setText(k.optString("name")+"\n"+k.optDouble("price")+" грн | "+k.optDouble("energy")+" кВт·год\n"+k.optString("note")+"\nЦіна введена вручну");t.setTextSize(18);t.setPadding(12,18,12,18);results.addView(t);}}
}