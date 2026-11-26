package ua.energykits.app;
import android.app.*; import android.os.*; import android.widget.*; import android.view.*; import android.graphics.Color; import android.content.*; import android.net.Uri; import org.json.*; import java.util.*;
public class MainActivity extends Activity {
 LinearLayout root, results, fieldHost; EditText inv,bat,ip,bp,iv,bv,ah,bms,watts; JSONArray kits=new JSONArray(); JSONObject inverterOffer, batteryOffer;
 TextView kitSummary; Button kitDetails, kitAlternatives; JSONObject previewInverter, previewBattery; java.util.List<JSONObject> foundInverters=Collections.emptyList(),foundBatteries=Collections.emptyList(); int searchGeneration=0;
 
 class EquipmentArt extends View {
 final int kind; android.graphics.Bitmap photo; final android.graphics.Paint p=new android.graphics.Paint(3);
 final android.graphics.RectF photoBounds=new android.graphics.RectF();
 final android.graphics.Path outline=new android.graphics.Path();
 EquipmentArt(int k){super(MainActivity.this);kind=k;try(java.io.InputStream in=getAssets().open(new String[]{"inverter.jpg","battery.jpg","solar_house.jpg"}[k])){photo=android.graphics.BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
 void rect(android.graphics.Canvas c,int color,float x,float y,float w,float h,float radius){p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,radius,radius,p);}
 void poly(android.graphics.Canvas c,int color,float... xy){p.setColor(color);outline.rewind();outline.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)outline.lineTo(xy[i],xy[i+1]);outline.close();c.drawPath(outline,p);}
 protected void onDraw(android.graphics.Canvas c){super.onDraw(c);if(photo!=null){float ratio=Math.min(getWidth()/(float)photo.getWidth(),getHeight()/(float)photo.getHeight());float w=photo.getWidth()*ratio,h=photo.getHeight()*ratio; photoBounds.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);c.drawBitmap(photo,null,photoBounds,p);return;}float scale=Math.min(getWidth()/100f,getHeight()/120f);c.save();c.translate((getWidth()-100*scale)/2,(getHeight()-120*scale)/2);c.scale(scale,scale);
 p.setColor(0x5522cfff);c.drawOval(2,18,98,110,p);
 if(kind==0){rect(c,0xff8294aa,13,12,76,99,8);rect(c,0xffedf5ff,9,9,75,97,8);rect(c,0xffc4d4e6,14,15,65,5,2);rect(c,0xff101b26,27,68,39,25,4);rect(c,0xff53c967,33,73,27,9,2);for(int i=0;i<3;i++)rect(c,0xffaab8c9,33+i*10,85,5,3,1);rect(c,0xff26394e,21,106,9,7,2);rect(c,0xff26394e,65,106,9,7,2);}
 else if(kind==1){rect(c,0xff101820,8,40,85,68,5);poly(c,0xff526276,8,40,20,28,94,28,93,40);rect(c,0xffd53030,22,23,12,9,2);rect(c,0xffc4d4e6,68,23,12,9,2);rect(c,0xff25374c,13,45,75,57,4);p.setColor(Color.WHITE);p.setTextSize(16);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(android.graphics.Paint.Align.CENTER);c.drawText("LiFePO4",50,76,p);p.setTextAlign(android.graphics.Paint.Align.LEFT);}
 else {rect(c,0xffad713b,19,53,70,53,2);poly(c,0xff25394c,7,56,50,14,98,56);poly(c,0xff1768ad,14,51,48,19,74,43,41,51);for(int i=0;i<3;i++)rect(c,0xffffd479,26+i*20,65,12,17,1);rect(c,0xff563a26,49,87,14,19,1);}
 c.restore();}
 }
 LinearLayout section(int kind){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);EquipmentArt image=new EquipmentArt(kind);row.addView(image,new LinearLayout.LayoutParams(dp(76),dp(82)));LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);row.addView(fields,new LinearLayout.LayoutParams(0,-2,1));space(row);return fields;}
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 android.graphics.drawable.GradientDrawable gloss(int top,int bottom){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(dp(12));d.setStroke(dp(1),0xff45d9ff);return d;}
 void space(View v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));root.addView(v,p);}
 EditText field(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setInputType(type);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffc6ddf5);e.setTextSize(16);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(gloss(0xff102e59,0xff071b3d));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(3),0,dp(3));fieldHost.addView(e,fp);return e;}
 void button(String name,Runnable action){Button b=new Button(this);b.setText(name);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(null,android.graphics.Typeface.BOLD);b.setMinHeight(dp(48));int top=0xff008dff,bottom=0xff003779;if(name.equals("Додати комплект")){top=0xff39b900;bottom=0xff126000;}if(name.equals("Вихід")){top=0xffff3939;bottom=0xff990000;}b.setBackgroundTintList(null);b.setBackground(gloss(top,bottom));space(b);b.setOnClickListener(v->action.run());}
 static double searchNumber(String raw,boolean optional){String value=raw.trim();if(optional&&value.isEmpty())return 0;double x=Double.parseDouble(value.replace(',','.'));if(Double.isNaN(x)||Double.isInfinite(x)||x<=0)throw new IllegalArgumentException();return x;}
 double num(EditText e){return searchNumber(e.getText().toString(),false);}
 double optionalNum(EditText e){return searchNumber(e.getText().toString(),true);}
 private static final String DEFAULT_FEED = "https://raw.githubusercontent.com/dim20041978-png/ENERGY-KITS-ANDROID/main/catalog/offers.json";
 private CompatibilityCatalog compatibility;
 private double requestedLoadKw = Double.NaN;
 private double requestedCapacityAh = 0;
 public void onCreate(Bundle b){super.onCreate(b);compatibility=new CompatibilityCatalog(getAssets());try{kits=new JSONArray(getPreferences(Context.MODE_PRIVATE).getString("kits","[]"));}catch(Exception ignored){}
 LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setPadding(dp(12),dp(8),dp(12),dp(8));screen.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff030e2c,0xff064e9b,0xff031330}));
 setContentView(screen);
 if(Build.VERSION.SDK_INT>=35){screen.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());view.setPadding(dp(12)+bars.left,dp(8)+bars.top,dp(12)+bars.right,dp(8)+bars.bottom);return insets;});screen.requestApplyInsets();}else{getWindow().setStatusBarColor(0xff030e2c);getWindow().setNavigationBarColor(0xff030e2c);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
 ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);s.addView(root);screen.addView(s,new LinearLayout.LayoutParams(-1,0,1));
 TextView title=new TextView(this);title.setText("⚡ Підбір енергокомплектів");title.setTextSize(20);title.setTextColor(Color.WHITE);title.setTypeface(null,android.graphics.Typeface.BOLD_ITALIC);title.setGravity(Gravity.CENTER);title.setPadding(dp(6),dp(8),dp(6),dp(8));title.setBackground(gloss(0xff006bd0,0xff041b50));space(title);
 fieldHost=section(0);watts=field("Потужність інвертора, кВт",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
 fieldHost=section(1);ah=field("Ємність акумулятора, А·год",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
 iv=field("Напруга системи, В",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);inv=new EditText(this);ip=new EditText(this);bat=new EditText(this);bp=new EditText(this);bv=new EditText(this);bms=new EditText(this);
 results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);results.setPadding(dp(8),dp(6),dp(8),dp(6));results.setBackground(gloss(0xff153d70,0xff081c3c));space(results);
 TextView heading=new TextView(this);heading.setText("ПІДІБРАНИЙ ЕНЕРГОКОМПЛЕКТ");heading.setTextColor(Color.WHITE);heading.setTextSize(16);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setGravity(Gravity.CENTER);results.addView(heading);
 kitSummary=new TextView(this);kitSummary.setTextColor(Color.WHITE);kitSummary.setTextSize(15);kitSummary.setPadding(dp(4),dp(5),dp(4),dp(4));results.addView(kitSummary);
 LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);results.addView(actions);
 kitDetails=summaryButton(actions,"Деталі",()->{if(previewInverter!=null&&previewBattery!=null)pairResult(previewInverter,previewBattery);});
 kitAlternatives=summaryButton(actions,"Інші пропозиції",()->showPairChoices(foundInverters,foundBatteries));
 pageButton(results,"Порівняти збережені",()->compareKits(previewInverter,previewBattery));
 resetPreview("Вкажіть потужність. Ємність і напруга — необов’язкові. Натисніть ВИБРАТИ.");
 android.text.TextWatcher invalidate=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int c,int a){}public void onTextChanged(CharSequence t,int st,int before,int count){searchGeneration++;requestedLoadKw=Double.NaN;resetPreview("Параметри змінено. Натисніть ВИБРАТИ.");}public void afterTextChanged(android.text.Editable t){}};
 for(EditText e:new EditText[]{watts,iv,ah})e.addTextChangedListener(invalidate);
 LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.VERTICAL);screen.addView(footer,new LinearLayout.LayoutParams(-1,-2));
 pageButton(footer,"ВИБРАТИ",()->{((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(watts.getWindowToken(),0);watts.clearFocus();ah.clearFocus();iv.clearFocus();guidedSearch();});
 pageButton(footer,"Вихід",()->{save();finish();});
 }
 Button summaryButton(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setBackgroundTintList(null);b.setBackground(gloss(0xff008dff,0xff003779));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),dp(2),dp(2),dp(2));parent.addView(b,lp);b.setOnClickListener(v->action.run());return b;}
 void resetPreview(String message){previewInverter=null;previewBattery=null;foundInverters=Collections.emptyList();foundBatteries=Collections.emptyList();kitSummary.setTextColor(Color.WHITE);kitSummary.setText(message);kitDetails.setEnabled(false);kitAlternatives.setEnabled(false);}
 void showPreview(JSONObject x,JSONObject y){previewInverter=x;previewBattery=y;double v=y.optDouble("voltage"),a=y.optDouble("ah");String energy=positiveFinite(v)&&positiveFinite(a)?String.format(Locale.US,"%.2f кВт·год | %s А·год",v*a/1000,y.optString("ah")):"немає даних";String warning=conflict(x,y);if(warning.isEmpty())warning="BMS: "+compatibilityEvidence(x,y);String summary=getString(R.string.kit_summary,x.optString("model")+" ("+x.optString("kw")+" кВт"+(x.optInt("phases")>0?", "+x.optInt("phases")+" ф.":"")+")",priceLabel(x),batteryName(y),priceLabel(y),y.optString("voltage"),energy,warning,totalPrice(x,y));
 kitSummary.setTextColor(Color.WHITE);
 if(isHigherPower(x.optDouble("kw"),requestedLoadKw)){
 String prefix=String.format(Locale.US,"АЛЬТЕРНАТИВА: %s кВт → %s кВт\n",powerText(requestedLoadKw),powerText(x.optDouble("kw")));
 android.text.SpannableString highlighted=new android.text.SpannableString(prefix+summary);
 int firstLine=summary.indexOf('\n');if(firstLine<0)firstLine=summary.length();
 highlighted.setSpan(new android.text.style.ForegroundColorSpan(0xffff6666),0,prefix.length()+firstLine,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 highlighted.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,prefix.length()-1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
 kitSummary.setText(highlighted);
 }else kitSummary.setText(summary);
 kitDetails.setEnabled(true);}
 static String powerText(double kw){return java.math.BigDecimal.valueOf(kw).stripTrailingZeros().toPlainString().replace('.',',');}
 static boolean isHigherPower(double actual,double requested){return CompatibilityMath.positiveFinite(requested)&&CompatibilityMath.positiveFinite(actual)&&actual-requested>1e-6;}
 static int compareKitPriority(JSONObject[] p,JSONObject[] q,double requested){
 if(CompatibilityMath.positiveFinite(requested)){
 double pd=Math.abs(p[0].optDouble("kw")-requested),qd=Math.abs(q[0].optDouble("kw")-requested);
 if(pd<=1e-6)pd=0;if(qd<=1e-6)qd=0;
 int byPower=Double.compare(pd,qd);if(byPower!=0)return byPower;
 }
 return Double.compare(p[0].optDouble("price")+p[1].optDouble("price"),q[0].optDouble("price")+q[1].optDouble("price"));
 }
 
 private final android.os.Handler autoHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private String lastAutoQuery="";
 private final Runnable autoSearch=()->{if(isFinishing()||isDestroyed())return;try{double k=num(watts),v=optionalNum(iv),a=optionalNum(ah);String key=k+":"+v+":"+a;if(key.equals(lastAutoQuery))return;lastAutoQuery=key;guidedSearch();}catch(Exception ignored){}};
 void installAutoSearch(){android.text.TextWatcher watcher=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){autoHandler.removeCallbacks(autoSearch);if(watts.hasFocus()||iv.hasFocus()||ah.hasFocus())autoHandler.postDelayed(autoSearch,1000);}public void afterTextChanged(android.text.Editable e){}};for(EditText e:new EditText[]{watts,iv,ah})e.addTextChangedListener(watcher);}
 JSONArray loadOffers(String endpoint) throws java.io.IOException, JSONException {
 try {
 return OfferSource.load(endpoint);
 } catch (java.io.IOException | JSONException failure) {
 if(!DEFAULT_FEED.equals(endpoint))throw failure;
 try(java.io.InputStream input=getAssets().open("offers.json")){
 return OfferSource.read(input);
 }
 }
 }
 void guidedSearch(){try{double kw=num(watts),v=optionalNum(iv),capacity=optionalNum(ah);String endpoint=getPreferences(Context.MODE_PRIVATE).getString("offer_feed",DEFAULT_FEED);final int generation=++searchGeneration;requestedLoadKw=kw;requestedCapacityAh=capacity;resetPreview("Пошук комплектів…");if(endpoint.isEmpty()){kitSummary.setText("Джерело цін не налаштоване.");kitAlternatives.setEnabled(true);return;}new Thread(()->{try{JSONArray offers=loadOffers(endpoint);java.util.List<JSONObject> i=OfferSource.matches(offers,"inverter",v,kw),a=OfferSource.matches(offers,"battery",v,Double.MIN_VALUE);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&generation==searchGeneration)automaticPairs(i,a);});}catch(Exception ex){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&generation==searchGeneration){lastAutoQuery="";kitSummary.setText("Не вдалося отримати пропозиції. Повторіть пошук або перевірте джерело цін.");kitAlternatives.setEnabled(true);}});}}).start();}catch(Exception e){Toast.makeText(this,"Вкажіть додатну потужність. Ємність і напругу можна залишити порожніми; введені значення мають бути додатними.",Toast.LENGTH_LONG).show();}}
 String batteryName(JSONObject o){int n=CompatibilityCatalog.modules(o);return (n>1?n+" × ":"")+o.optString("model");}
 JSONObject bankOffer(JSONObject unit,int count){
 try{if(count<1)return null;JSONObject bank=new JSONObject(unit.toString());
 bank.remove("_kit_modules");bank.put("_kit_modules",count);
 bank.put("ah",unit.optDouble("ah")*count);bank.put("price",unit.optDouble("price")*count);
 if(unit.has("price_max")){double max=unit.optDouble("price_max");if(positiveFinite(max))bank.put("price_max",max*count);}
 return bank;}catch(JSONException ex){return null;}}
 java.util.List<JSONObject[]> compatiblePairs(java.util.List<JSONObject> inverters,java.util.List<JSONObject> batteries){
 java.util.List<JSONObject[]> pairs=new ArrayList<>();
 for(JSONObject x:inverters)for(JSONObject y:batteries){
 int count=compatibility.minimumModules(x,y,loadFor(x),requestedCapacityAh);if(count<1)continue;
 JSONObject bank=bankOffer(y,count);if(bank==null||!compatibility.canRecommend(x,bank,loadFor(x)))continue;
 pairs.add(new JSONObject[]{x,bank});}
 Collections.sort(pairs,(p,q)->compareKitPriority(p,q,requestedLoadKw));
 java.util.Set<String> seen=new HashSet<>();
 java.util.Iterator<JSONObject[]> iterator=pairs.iterator();while(iterator.hasNext()){JSONObject[] p=iterator.next();String key=p[0].optString("model")+"\n"+p[1].optString("model");if(!seen.add(key))iterator.remove();}
 return pairs;}
 void automaticPairs(java.util.List<JSONObject> inverters,java.util.List<JSONObject> batteries){foundInverters=inverters;foundBatteries=batteries;kitAlternatives.setEnabled(true);java.util.List<JSONObject[]> pairs=compatiblePairs(inverters,batteries);if(pairs.isEmpty()){kitSummary.setText(R.string.no_confirmed_kit);return;}JSONObject[] best=pairs.get(0);showPreview(best[0],best[1]);}
 interface PriceChange{void change(long low,long high);}
 class PriceSlider extends View{
 final long min,max;long low,high;int active=-1;float downX;final android.graphics.Paint paint=new android.graphics.Paint(3);final PriceChange callback;
 PriceSlider(long min,long max,PriceChange cb){super(MainActivity.this);this.min=min;this.max=max;low=min;high=max;callback=cb;setContentDescription("Ціновий діапазон. Для точного введення використовуйте поля Від і До.");setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);}
 float left(){return dp(20);}float right(){return Math.max(left()+1,getWidth()-dp(20));}
 float x(long v){return max<=min?left():left()+(right()-left())*(float)Math.max(0,Math.min(1,(v-(double)min)/(max-(double)min)));}
 long value(float x){return KitPriceRange.position(min,max,(x-left())/(right()-left()));}
 void range(long a,long b){low=a;high=b;invalidate();}
 @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float y=getHeight()/2f,lo=x(low),hi=x(high);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeWidth(dp(5));paint.setColor(0xff456789);canvas.drawLine(left(),y,right(),y,paint);paint.setColor(0xff55d9ff);canvas.drawLine(lo,y,hi,y,paint);paint.setColor(0xff8ce5ff);canvas.drawCircle(lo,y,dp(10),paint);canvas.drawCircle(hi,y,dp(10),paint);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(Color.WHITE);canvas.drawCircle(lo,y,dp(10),paint);canvas.drawCircle(hi,y,dp(10),paint);paint.setStyle(android.graphics.Paint.Style.FILL);}
 @Override public boolean onTouchEvent(android.view.MotionEvent e){
 if(max<=min)return false;float px=e.getX();
 switch(e.getActionMasked()){
 case MotionEvent.ACTION_DOWN:downX=px;float dl=Math.abs(px-x(low)),dh=Math.abs(px-x(high));active=dl==dh?-2:dl<dh?0:1;getParent().requestDisallowInterceptTouchEvent(true);return true;
 case MotionEvent.ACTION_MOVE:case MotionEvent.ACTION_UP:
 if(active==-2){if(px==downX&&e.getActionMasked()==MotionEvent.ACTION_MOVE)return true;active=px<x(low)?0:1;}
 if(active>=0){long v=value(px);long l=Math.max(min,Math.min(max,low)),h=Math.max(min,Math.min(max,high));if(active==0)l=Math.min(v,h);else h=Math.max(v,l);range(l,h);callback.change(l,h);}
 if(e.getActionMasked()==MotionEvent.ACTION_UP){active=-1;getParent().requestDisallowInterceptTouchEvent(false);performClick();}return true;
 case MotionEvent.ACTION_CANCEL:active=-1;getParent().requestDisallowInterceptTouchEvent(false);return true;
 default:return true;
 }}
 @Override public boolean performClick(){super.performClick();return true;}
 }
 void showPairChoices(java.util.List<JSONObject> inverters,java.util.List<JSONObject> batteries){
 Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,"Готові енергокомплекти");
 java.util.List<JSONObject[]> pairs=compatiblePairs(inverters,batteries);
 long minimum=Long.MAX_VALUE,maximum=0;for(JSONObject[] pair:pairs){long p=KitPriceRange.total(pair);if(p>=0){minimum=Math.min(minimum,p);maximum=Math.max(maximum,p);}}
 final boolean hasPrices=minimum!=Long.MAX_VALUE;final long min=hasPrices?minimum:0,max=hasPrices?maximum:0;
 LinearLayout filter=new LinearLayout(this);filter.setOrientation(LinearLayout.VERTICAL);filter.setPadding(dp(10),dp(10),dp(10),dp(10));filter.setBackground(gloss(0xff153d70,0xff081c3c));body.addView(filter);
 TextView filterTitle=applianceText("ВАРТІСТЬ КОМПЛЕКТУ",17);filterTitle.setGravity(Gravity.CENTER);filter.addView(filterTitle);
 LinearLayout rangeFields=new LinearLayout(this);filter.addView(rangeFields);
 LinearLayout from=new LinearLayout(this),to=new LinearLayout(this);from.setOrientation(LinearLayout.VERTICAL);to.setOrientation(LinearLayout.VERTICAL);
 LinearLayout.LayoutParams fromLp=new LinearLayout.LayoutParams(0,-2,1);fromLp.setMargins(0,dp(6),dp(4),0);rangeFields.addView(from,fromLp);
 LinearLayout.LayoutParams toLp=new LinearLayout.LayoutParams(0,-2,1);toLp.setMargins(dp(4),dp(6),0,0);rangeFields.addView(to,toLp);
 from.addView(applianceText("Від, грн",13));to.addView(applianceText("До, грн",13));
 EditText lower=applianceInput(KitPriceRange.format(min),"Мінімальна ціна, грн"),upper=applianceInput(KitPriceRange.format(max),"Максимальна ціна, грн");
 from.addView(lower,new LinearLayout.LayoutParams(-1,-2));to.addView(upper,new LinearLayout.LayoutParams(-1,-2));
 TextView count=applianceText("",15);TextView note=applianceText("",13);note.setTextColor(0xffc6ddf5);
 LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
 TextView empty=applianceText("",16);LinearLayout[] cards=new LinearLayout[pairs.size()];TextView[] labels=new TextView[pairs.size()];
 final boolean[] internal={false};
 final long[] applied={min,max};
 Runnable render=()->{
 long lo=applied[0],hi=applied[1];int matches=0,shown=0,childIndex=0;
 for(int i=0;i<pairs.size();i++){
 JSONObject[] pair=pairs.get(i);long price=KitPriceRange.total(pair);boolean match=KitPriceRange.includes(price,lo,hi);
 if(match)matches++;boolean show=match&&shown<100;
 if(show){shown++;
 if(cards[i]==null){JSONObject x=pair[0],y=pair[1];LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);cards[i]=card;
 TextView heading=applianceText("",22);heading.setPadding(dp(12),dp(12),dp(12),dp(12));labels[i]=heading;card.addView(heading);
 offerPhoto(card,x);pageText(card,offerLabel(x),18);offerPhoto(card,y);pageText(card,offerLabel(y),18);pageText(card,"Разом: "+totalPrice(x,y),22);pageText(card,pairCheck(x,y),16);pageButton(card,"Вибрати комплект",()->{d.dismiss();pairResult(x,y);});list.addView(card,childIndex);}
 labels[i].setText("Комплект №"+shown);}
 if(cards[i]!=null){cards[i].setVisibility(show?View.VISIBLE:View.GONE);childIndex++;}
 }
 count.setText("Знайдено: "+matches+" із "+pairs.size()+" комплектів"+(matches>100?" (показано 100)":""));
 empty.setText(pairs.isEmpty()?"Автоматично підтверджених комплектів немає. Доступний ручний підбір.":"У вибраному ціновому діапазоні комплектів немає.");
 empty.setVisibility(matches==0?View.VISIBLE:View.GONE);
 };
 PriceSlider slider=new PriceSlider(min,max,(lo,hi)->{internal[0]=true;lower.setText(KitPriceRange.format(lo));upper.setText(KitPriceRange.format(hi));lower.setError(null);upper.setError(null);internal[0]=false;applied[0]=lo;applied[1]=hi;render.run();});
 filter.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));
 note.setText("Фільтр за сумою інвертора та всіх АКБ. Для орієнтовних цін — за показаною базовою сумою.");
 filter.addView(note);pageButton(filter,"СКИНУТИ ФІЛЬТР",()->{internal[0]=true;lower.setText(KitPriceRange.format(min));upper.setText(KitPriceRange.format(max));lower.setError(null);upper.setError(null);internal[0]=false;applied[0]=min;applied[1]=max;slider.range(min,max);render.run();});
 filter.addView(count);body.addView(empty);pageText(body,"Спочатку точна потужність, потім найближча більша; у межах потужності — за ціною. Ціна обладнання без монтажу, захисту та шин. Перевірте фазність і паспортні умови.",14);body.addView(list);
 android.text.TextWatcher watcher=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int n){
 if(internal[0])return;long lo=KitPriceRange.parse(lower.getText().toString()),hi=KitPriceRange.parse(upper.getText().toString());lower.setError(lo<0?"Вкажіть суму від 0":null);upper.setError(hi<0?"Вкажіть суму від 0":lo>=0&&hi<lo?"Сума До має бути не меншою за Від":null);
 if(lo<0||hi<lo){count.setText("Некоректний діапазон — показано попередній результат");return;}
 applied[0]=lo;applied[1]=hi;slider.range(lo,hi);render.run();}
 public void afterTextChanged(android.text.Editable e){}};
 lower.addTextChangedListener(watcher);upper.addTextChangedListener(watcher);lower.setEnabled(hasPrices);upper.setEnabled(hasPrices);slider.setEnabled(hasPrices);
 body.setFocusableInTouchMode(true);body.requestFocus();
 pageButton(body,"Ручний підбір",()->{d.dismiss();choosePair(inverters,batteries);});pageButton(body,"Оновити ціни",()->{d.dismiss();guidedSearch();});pageButton(body,"Назад",d::dismiss);render.run();d.show();
 }
 void choosePair(java.util.List<JSONObject> i,java.util.List<JSONObject> a){Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,"Результати підбору");JSONObject[] selected=new JSONObject[2];TextView summary=new TextView(this);summary.setTextColor(Color.WHITE);summary.setTextSize(18);summary.setText("Виберіть інвертор та акумулятор");body.addView(summary);Runnable advance=()->{if(selected[0]!=null&&selected[1]!=null&&d.isShowing()){d.dismiss();pairResult(selected[0],selected[1]);}};Runnable update=()->summary.setText(getString(R.string.manual_pair_summary,selected[0]==null?"не вибрано":selected[0].optString("model"),selected[1]==null?"не вибрано":selected[1].optString("model")));pageText(body,"Інвертори",22);if(i.isEmpty())pageText(body,"Пропозицій не знайдено",16);for(JSONObject o:i){offerPhoto(body,o);pageText(body,offerLabel(o),18);pageButton(body,"Вибрати інвертор",()->{if(selected[1]!=null&&!conflict(o,selected[1]).isEmpty()){Toast.makeText(this,conflict(o,selected[1]),Toast.LENGTH_LONG).show();return;}selected[0]=o;update.run();advance.run();});}pageText(body,"Акумулятори",22);if(a.isEmpty())pageText(body,"Пропозицій не знайдено",16);for(JSONObject o:a){offerPhoto(body,o);pageText(body,offerLabel(o),18);pageButton(body,"Вибрати акумулятор",()->{if(selected[0]!=null&&!conflict(selected[0],o).isEmpty()){Toast.makeText(this,conflict(selected[0],o),Toast.LENGTH_LONG).show();return;}selected[1]=o;update.run();advance.run();});}if(i.isEmpty()||a.isEmpty()){pageText(body,"Джерело цін не налаштоване, недоступне або не містить відповідних товарів. Ціни не вигадуються.",16);pageButton(body,"Налаштувати джерело цін",()->{d.dismiss();lastAutoQuery="";configureFeed();});}pageButton(body,"Назад",d::dismiss);d.show();}
 interface OfferChoice{void select(JSONObject o);}
 LinearLayout page(Dialog d,String title){
 FrameLayout safe=new FrameLayout(this);safe.setBackgroundColor(0xff030e2c);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout body=new LinearLayout(this);
 body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(20),dp(12),dp(20));body.setBackground(gloss(0xff064e9b,0xff030e2c));scroll.addView(body);
 safe.addView(scroll,new FrameLayout.LayoutParams(-1,-1));d.setContentView(safe);
 Window window=d.getWindow();
 if(window!=null){
 window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
 if(Build.VERSION.SDK_INT>=30){
 window.setDecorFitsSystemWindows(false);
 safe.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;});
 safe.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){v.requestApplyInsets();}public void onViewDetachedFromWindow(View v){}});
 safe.requestApplyInsets();
 }else{window.setStatusBarColor(0xff030e2c);window.setNavigationBarColor(0xff030e2c);}
 }
 pageText(body,title,23);return body;
 }
 void pageText(LinearLayout body,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(dp(12),dp(12),dp(12),dp(12));body.addView(t);}
 void pageButton(LinearLayout body,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundTintList(null);b.setBackground(label.equals("Вихід")?gloss(0xffff3939,0xff990000):gloss(0xff008dff,0xff003779));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));body.addView(b,lp);b.setOnClickListener(v->action.run());}
 String priceLabel(JSONObject offer){
 String price=offer.optString("price");
 if("manual".equals(offer.optString("price_source")))return price+" грн (введено вручну)";
 if("direct_verified".equals(offer.optString("price_status"))){
 String availability=offer.optString("availability");
 return price+" грн"+("preorder".equals(availability)?" (передзамовлення)":"");
 }
 if("product_aggregate".equals(offer.optString("price_status")))return "від "+price+" грн (агрегатор)";
 String maximum=offer.optString("price_max");
 return (maximum.isEmpty()?price:price+"–"+maximum)+" грн (орієнтовно)";
 }
 String totalPrice(JSONObject inverter,JSONObject battery){
 String total=String.format(Locale.US,"%.2f грн",inverter.optDouble("price")+battery.optDouble("price"));
 boolean bothDirect="direct_verified".equals(inverter.optString("price_status"))&&"direct_verified".equals(battery.optString("price_status"));
 boolean bothManual="manual".equals(inverter.optString("price_source"))&&"manual".equals(battery.optString("price_source"));
 return bothDirect||bothManual?total:total+" (орієнтовно)";
 }
 String offerLabel(JSONObject o){return batteryName(o)+" — "+priceLabel(o)+"\n"+o.optString("voltage")+" В | "+("inverter".equals(o.optString("kind"))?o.optString("kw")+" кВт"+(o.optInt("phases")>0?" | "+o.optInt("phases")+" ф.":""):o.optString("ah")+" А·год")+"\n"+o.optString("seller")+" | "+o.optString("updated_at");}
 void chooseOffer(String title,java.util.List<JSONObject> list,OfferChoice choice){Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,title);for(JSONObject o:list){offerPhoto(body,o);pageText(body,offerLabel(o),18);pageButton(body,"Вибрати",()->{d.dismiss();choice.select(o);});pageButton(body,"Сторінка продавця",()->openOffer(o));}pageButton(body,"Назад",d::dismiss);d.show();}
 final java.util.concurrent.ExecutorService photoPool=java.util.concurrent.Executors.newFixedThreadPool(2);
 void offerPhoto(LinearLayout body,JSONObject o){
 String asset=o.optString("image_asset");
 if(!asset.isEmpty()){
 try(java.io.InputStream input=getAssets().open(asset)){
 android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(input);
 if(bitmap!=null){
 ImageView local=new ImageView(this);
 local.setImageBitmap(bitmap);
 local.setScaleType(ImageView.ScaleType.FIT_CENTER);
 local.setContentDescription("Фото: "+o.optString("model"));
 body.addView(local,new LinearLayout.LayoutParams(-1,dp(150)));
 return;
 }
 }catch(java.io.IOException ignored){}
 }
 String url=o.optString("image_url","");if(!url.startsWith("https://"))return;ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("Фото: "+o.optString("model"));image.setVisibility(View.GONE);body.addView(image,new LinearLayout.LayoutParams(-1,dp(150)));photoPool.execute(()->{java.net.HttpURLConnection conn=null;try{conn=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();conn.setConnectTimeout(5000);conn.setReadTimeout(7000);conn.setInstanceFollowRedirects(false);if(conn.getResponseCode()!=200)return;java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();try(java.io.InputStream in=conn.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2000000)return;out.write(buf,0,n);}}byte[] data=out.toByteArray();android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(data,0,data.length,opts);if(opts.outWidth<=0||opts.outHeight<=0)return;opts.inSampleSize=1;while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>800)opts.inSampleSize*=2;opts.inJustDecodeBounds=false;android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(data,0,data.length,opts);if(bitmap!=null)runOnUiThread(()->{if(!isFinishing()&&image.isAttachedToWindow()){image.setImageBitmap(bitmap);image.setVisibility(View.VISIBLE);}});}catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}});}
 @Override protected void onDestroy(){autoHandler.removeCallbacks(autoSearch);photoPool.shutdownNow();super.onDestroy();}
 void offerLink(LinearLayout body,JSONObject offer){
 String gallery=offer.optString("photo_page");
 if(gallery.startsWith("https://")&&!gallery.equals(offer.optString("url"))){
 pageButton(body,"Фото в джерелі",()->openHttps(gallery));
 }
 if(offer.optString("url").startsWith("https://")){
 pageButton(body,"direct_verified".equals(offer.optString("price_status"))?"Товар у продавця":"Джерело пропозиції",()->openOffer(offer));
 }
 }
 void openOffer(JSONObject o){openHttps(o.optString("url"));}
 void openHttps(String url){try{Uri u=Uri.parse(url);if("https".equalsIgnoreCase(u.getScheme()))startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}}
 boolean positiveFinite(double v){return CompatibilityMath.positiveFinite(v);}
 double loadFor(JSONObject inverter){
 return positiveFinite(requestedLoadKw)?requestedLoadKw:inverter.optDouble("kw");
 }
 double continuousCurrent(JSONObject battery){
 JSONObject spec=compatibility.specification(battery);
 return spec==null?Double.NaN:spec.optDouble("max_discharge_a")*CompatibilityCatalog.modules(battery);
 }
 String compatibilityEvidence(JSONObject inverter,JSONObject battery){
 JSONObject rule=compatibility.bmsRule(inverter,battery);
 if(rule==null)return "точна сумісність не підтверджена";
 return rule.optString("protocol")+". "+rule.optString("conditions");
 }
 String conflict(JSONObject inverter,JSONObject battery){
 return compatibility.conflict(inverter,battery,loadFor(inverter));
 }
 String pairCheck(JSONObject inverter,JSONObject battery){
 JSONObject i=compatibility.specification(inverter),b=compatibility.specification(battery);
 double load=loadFor(inverter);
 StringBuilder result=new StringBuilder("BMS: ").append(compatibilityEvidence(inverter,battery));
 if(i!=null&&b!=null&&CompatibilityMath.validRange(i.optDouble("dc_min"),i.optDouble("dc_max"))
         &&CompatibilityMath.validRange(b.optDouble("voltage_min"),b.optDouble("voltage_max"))){
 result.append("\nДіапазон DC інвертора: ").append(i.optString("dc_min")).append("–")
       .append(i.optString("dc_max")).append(" В, АКБ: ").append(b.optString("voltage_min"))
       .append("–").append(b.optString("voltage_max")).append(" В");
 }else{
 result.append("\nНомінальна напруга: ").append(inverter.optString("voltage")).append(" / ")
       .append(battery.optString("voltage")).append(" В. Повні діапазони DC не підтверджено.");
 }
 double required=b==null?Double.NaN:CompatibilityMath.requiredCurrent(load,b.optDouble("voltage_min"),b.optDouble("voltage"));
 if(positiveFinite(required)&&positiveFinite(continuousCurrent(battery))){
 result.append(String.format(Locale.US,"\nДля %.2f кВт потрібно близько %.1f А із запасом. Межа розряду АКБ: %.1f А.",
       load,required,continuousCurrent(battery)));
 }else result.append("\nНедостатньо паспортних даних для перевірки струму розряду.");
 int modules=CompatibilityCatalog.modules(battery);
 if(modules>1)result.append("\nБанк: ").append(modules).append(" однакових АКБ паралельно. Ціна без кабелів, захисту, шин та монтажу. Перевірити проєкт, рівномірність струмів, BMS і температурні обмеження.");
 double limit=CompatibilityCatalog.chargeLimit(i,b);
 if(modules>1&&i!=null&&b!=null&&positiveFinite(limit))limit=Math.min(i.optDouble("max_charge_a"),b.optDouble("max_charge_a")*modules);
 if(positiveFinite(limit)){
 result.append("\nВерхня межа струму заряду: ").append(limit)
       .append(" А. Враховуйте нижчі ліміти BMS та паспортні умови.");
 }else result.append("\nМежу струму заряду не підтверджено.");
 if(i!=null&&!i.optString("spec_note").isEmpty())result.append("\n").append(i.optString("spec_note"));
 if(b!=null&&!b.optString("spec_note").isEmpty())result.append("\n").append(b.optString("spec_note"));
 String problem=conflict(inverter,battery);
 if(!problem.isEmpty())result.append("\n").append(problem);
 return result.toString();
 }
 boolean savePair(JSONObject x,JSONObject y){if(!conflict(x,y).isEmpty()){Toast.makeText(this,conflict(x,y),Toast.LENGTH_LONG).show();return false;}try{double i=x.optDouble("price"),b=y.optDouble("price");if(!(i>0&&b>0)||Double.isInfinite(i+b))throw new IllegalArgumentException();JSONObject k=new JSONObject();k.put("name",x.optString("model")+" + "+batteryName(y));k.put("price",i+b);double v=y.optDouble("voltage",0),a=y.optDouble("ah",0);k.put("energy",v*a/1000);k.put("note",pairCheck(x,y)+applianceReport(x,y));k.put("inverter_source",x.optString("seller")+" | "+x.optString("updated_at")+" | "+x.optString("url"));k.put("battery_source",y.optString("seller")+" | "+y.optString("updated_at")+" | "+y.optString("url"));k.put("inverter_offer",x);k.put("battery_offer",y);kits.put(k);save();render();return true;}catch(Exception ex){Toast.makeText(this,"Не вдалося зберегти комплект",Toast.LENGTH_LONG).show();return false;}}
 JSONObject exactOffer(java.util.List<JSONObject> list,JSONObject old){
 JSONObject alternative=null;
 for(JSONObject o:list){
 if(!o.optString("model").equals(old.optString("model"))||!o.optString("seller").equals(old.optString("seller")))continue;
 if(o.optString("url").equals(old.optString("url")))return o;
 if(alternative!=null)return null;
 alternative=o;
 }
 return alternative;
 }
 void refreshPair(Dialog d,JSONObject x,JSONObject y){String endpoint=getPreferences(Context.MODE_PRIVATE).getString("offer_feed",DEFAULT_FEED);if(endpoint.isEmpty()){Toast.makeText(this,"Налаштуйте джерело цін",Toast.LENGTH_LONG).show();return;}Toast.makeText(this,"Перевірка цін…",Toast.LENGTH_SHORT).show();new Thread(()->{try{JSONArray offers=OfferSource.load(endpoint);JSONObject nx=exactOffer(OfferSource.matches(offers,"inverter",x.optDouble("voltage"),x.optDouble("kw")),x),ny=exactOffer(OfferSource.matches(offers,"battery",y.optDouble("voltage"),y.optDouble("ah")/CompatibilityCatalog.modules(y)),y);final JSONObject refreshed=ny==null?null:bankOffer(ny,CompatibilityCatalog.modules(y));runOnUiThread(()->{if(isFinishing()||!d.isShowing())return;if(nx==null||refreshed==null||!compatibility.canRecommend(nx,refreshed,loadFor(nx))){Toast.makeText(this,"Не знайдено обидві вихідні пропозиції. Попередні ціни збережено.",Toast.LENGTH_LONG).show();return;}d.dismiss();pairResult(nx,refreshed);});}catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&d.isShowing())Toast.makeText(this,"Не вдалося оновити ціни. Попередні дані збережено.",Toast.LENGTH_LONG).show();});}}).start();}
 void printPair(JSONObject x,JSONObject y){
 android.print.PrintManager manager=(android.print.PrintManager)getSystemService(Context.PRINT_SERVICE);
 if(manager==null){Toast.makeText(this,"Служба друку недоступна",Toast.LENGTH_LONG).show();return;}
 StringBuilder report=new StringBuilder("Підбір енергокомплектів 1.0.0\n\n")
       .append(offerLabel(x)).append("\n").append(x.optString("url")).append("\n\n")
       .append(offerLabel(y)).append("\n").append(y.optString("url"))
       .append("\n\nРазом: ").append(totalPrice(x,y))
       .append("\n\n").append(pairCheck(x,y)).append(applianceReport(x,y));
 JSONObject inverterSpec=compatibility.specification(x),batterySpec=compatibility.specification(y);
 JSONObject bmsRule=compatibility.bmsRule(x,y);
 if(inverterSpec!=null)report.append("\n\nПаспорт інвертора\n").append(inverterSpec.optString("spec_source"));
 if(batterySpec!=null)report.append("\n\nПаспорт АКБ\n").append(batterySpec.optString("spec_source"));
 if(bmsRule!=null){
 report.append("\n\nДжерело BMS\n").append(bmsRule.optString("source"));
 if(bmsRule.has("setup_source"))report.append("\n\nНалаштування BMS\n").append(bmsRule.optString("setup_source"));
 }
 manager.print("Енергокомплект",new KitPrintAdapter(this,report.toString()),
       new android.print.PrintAttributes.Builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build());
 }
 void manualPrices(Dialog previous,JSONObject x,JSONObject y){
 Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,"Ручне введення цін");
 pageText(body,"Введіть перевірені вами ціни у гривнях. Вони будуть позначені як ручні, а не автоматично отримані.",16);
 LinearLayout old=fieldHost;fieldHost=body;
 pageText(body,x.optString("model"),18);EditText first=field("Ціна інвертора, грн",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
 pageText(body,batteryName(y),18);EditText second=field("Загальна ціна всіх АКБ, грн",android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
 fieldHost=old;
 pageButton(body,"Застосувати",()->{try{
 double a=num(first),b=num(second);if(Double.isInfinite(a+b))throw new IllegalArgumentException();
 JSONObject nx=new JSONObject(x.toString()),ny=new JSONObject(y.toString());
 String date=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date());
 for(JSONObject o:new JSONObject[]{nx,ny}){o.put("price",o==nx?a:b);o.put("seller","Ручне введення");o.put("updated_at",date);o.put("price_source","manual");o.remove("url");o.remove("price_status");}
 d.dismiss();previous.dismiss();pairResult(nx,ny);
 }catch(Exception ex){Toast.makeText(this,"Введіть дві коректні додатні ціни",Toast.LENGTH_LONG).show();}});
 pageButton(body,"Скасувати",d::dismiss);d.show();
 }
 String applianceKey(JSONObject x,JSONObject y){
 return "appliances_v1:"+x.optString("model")+"|"+y.optString("model")+"|"+CompatibilityCatalog.modules(y)+"|"+loadFor(x);
 }
 JSONObject applianceProfile(JSONObject x,JSONObject y){
 try{return new JSONObject(getPreferences(Context.MODE_PRIVATE).getString(applianceKey(x,y),"{}"));}catch(JSONException ex){return new JSONObject();}
 }
 double applianceLimit(JSONObject x,JSONObject y){
 double requested=loadFor(x);
 return positiveFinite(requested)&&compatibility.canRecommend(x,y,requested)?requested*1000:Double.NaN;
 }
 double applianceEnergy(JSONObject y){return y.optDouble("voltage")*y.optDouble("ah");}
 String applianceNumber(double n){return String.format(Locale.US,"%.2f",n).replaceAll("0+$","").replaceAll("\\.$","").replace('.',',');}
 TextView applianceText(String value,int size){
 TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;
 }
 TextView applianceMetric(LinearLayout parent,String label){
 View divider=new View(this);divider.setBackgroundColor(0xff34618b);LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(-1,dp(1));sep.setMargins(0,dp(8),0,dp(6));parent.addView(divider,sep);
 TextView caption=applianceText(label,13);caption.setTextColor(0xffc6ddf5);parent.addView(caption);
 TextView value=applianceText("—",20);value.setTypeface(null,android.graphics.Typeface.BOLD);parent.addView(value);return value;
 }
 EditText applianceInput(String value,String description){
 EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setTextSize(14);
 e.setGravity(Gravity.CENTER);e.setPadding(dp(3),dp(7),dp(3),dp(7));e.setBackground(gloss(0xff102e59,0xff071b3d));
 e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
 e.setContentDescription(description);e.setText(value);return e;
 }
 String applianceReport(JSONObject x,JSONObject y){
 JSONObject profile=applianceProfile(x,y);
 JSONArray rows=profile.optJSONArray("rows");if(rows==null||rows.length()!=ApplianceUsage.NAMES.length)return "";
 double w=0,wh=0;StringBuilder out=new StringBuilder("\n\nЕлектроприлади та автономність (орієнтовно)\n");
 boolean start=false;int selected=0;
 for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null||!row.optBoolean("on"))continue;
 double p=row.optDouble("w"),h=row.optDouble("h");if(!positiveFinite(p)||h<0||h>24||Double.isNaN(h))return "";
 selected++;w+=p;wh+=ApplianceUsage.dailyWh(p,h);start|=ApplianceUsage.START_CHECK[i];
 out.append(ApplianceUsage.NAMES[i]).append(": ").append(applianceNumber(p)).append(" Вт; ").append(applianceNumber(h)).append(" год/добу\n");}
 if(selected==0)return "";
 double limit=applianceLimit(x,y),runtime=ApplianceUsage.runtimeHours(applianceEnergy(y),w);
 out.append("Одночасне навантаження: ").append(applianceNumber(w)).append(" Вт\nДобове споживання: ").append(applianceNumber(wh/1000)).append(" кВт·год");
 if(positiveFinite(limit)&&w<=limit&&positiveFinite(runtime))out.append("\nАвтономність при одночасній роботі: ").append(ApplianceUsage.runtimeText(runtime));
 else out.append("\nАвтономність не підтверджено: перевірте навантаження та сумісність.");
 double usable=ApplianceUsage.usableWh(applianceEnergy(y)),balance=ApplianceUsage.dailyBalanceWh(applianceEnergy(y),wh);
 if(positiveFinite(usable)&&!Double.isNaN(balance)){out.append("\nДоступна енергія АКБ: ").append(applianceNumber(usable/1000)).append(" кВт·год\nПотреба АКБ на добу з втратами: ").append(applianceNumber(ApplianceUsage.dailyBatteryWh(wh)/1000)).append(" кВт·год");
 out.append(balance<0?"\nЕнергії на добу недостатньо. Дефіцит: ":"\nЗа енергією на добу достатньо. Запас: ").append(applianceNumber(Math.abs(balance)/1000)).append(" кВт·год");}
 if(start)out.append("\nПускові навантаження потребують окремої паспортної перевірки.");
 if(x.optInt("phases")!=1)out.append("\nПеревірте фазність і допустиме навантаження кожної фази.");
 out.append("\nОцінка: 90% доступної ємності, ККД 90%, власне споживання 30 Вт протягом 24 годин, без підзаряджання.");
 return out.toString();
 }
 void applianceScreen(JSONObject x,JSONObject y){
 Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);
 LinearLayout body=page(d,"⚡ ЕЛЕКТРОПРИЛАДИ ТА АВТОНОМНІСТЬ");
 double limit=applianceLimit(x,y),energy=applianceEnergy(y);
 LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),dp(10),dp(12),dp(10));info.setBackground(gloss(0xff153d70,0xff081c3c));body.addView(info);
 pageText(info,"ХАРАКТЕРИСТИКИ СИСТЕМИ\nІнвертор: "+x.optString("model")+" ("+x.optString("kw")+" кВт)\nАКБ: "+batteryName(y)+"\n"+y.optString("voltage")+" В / "+y.optString("ah")+" А·год\nЕнергоємність: "+(positiveFinite(energy)?applianceNumber(energy/1000)+" кВт·год":"невідома")+"\nРозрахункове навантаження: "+(positiveFinite(limit)?applianceNumber(limit/1000)+" кВт":"не підтверджено"),16);
 pageText(body,"Позначте прилади. Змінюйте Вт та години прямо в таблиці. Значення за замовчуванням орієнтовні; використовуйте фактичну споживану потужність.",14);
 LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
 TextView h1=applianceText("Електроприлад",13),h2=applianceText("Вт",13),h3=applianceText("Год/добу",13);h2.setGravity(Gravity.CENTER);h3.setGravity(Gravity.CENTER);
 header.addView(h1,new LinearLayout.LayoutParams(0,-2,1));header.addView(h2,new LinearLayout.LayoutParams(dp(65),-2));header.addView(h3,new LinearLayout.LayoutParams(dp(65),-2));body.addView(header);
 int count=ApplianceUsage.NAMES.length;EditText[] powers=new EditText[count],hours=new EditText[count];CheckBox[] selected=new CheckBox[count];
 TextView[] indicators=new TextView[count];
 LinearLayout totals=new LinearLayout(this);totals.setOrientation(LinearLayout.VERTICAL);totals.setPadding(dp(10),dp(12),dp(10),dp(12));totals.setBackground(gloss(0xff153d70,0xff081c3c));
 TextView resultTitle=applianceText("РЕЗУЛЬТАТ РОЗРАХУНКУ",17);resultTitle.setGravity(Gravity.CENTER);resultTitle.setTypeface(null,android.graphics.Typeface.BOLD);totals.addView(resultTitle);
 TextView loadValue=applianceMetric(totals,"Загальне одночасне навантаження"),dailyValue=applianceMetric(totals,"Добове споживання"),runtimeValue=applianceMetric(totals,"Автономність при одночасній роботі"),usableValue=applianceMetric(totals,"Доступна енергія АКБ"),requiredValue=applianceMetric(totals,"Потреба АКБ на добу з втратами");
 ProgressBar coverage=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);coverage.setMax(1000);coverage.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff783c45));coverage.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff42bc72));totals.addView(coverage,new LinearLayout.LayoutParams(-1,dp(12)));
 TextView balanceValue=applianceText("",15);balanceValue.setPadding(0,dp(6),0,dp(6));totals.addView(balanceValue);
 TextView warnings=applianceText("",14);warnings.setPadding(0,dp(6),0,0);totals.addView(warnings);
 JSONArray stored=applianceProfile(x,y).optJSONArray("rows");
 for(int i=0;i<count;i++){
 JSONObject old=stored==null?null:stored.optJSONObject(i);LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(0,dp(6),0,dp(6));
 CheckBox check=new CheckBox(this);check.setTextColor(Color.WHITE);check.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff67d9ff));check.setContentDescription("Включити "+ApplianceUsage.NAMES[i]);check.setChecked(old==null?i<5:old.optBoolean("on"));selected[i]=check;line.addView(check,new LinearLayout.LayoutParams(dp(42),-2));
 LinearLayout nameBlock=new LinearLayout(this);nameBlock.setOrientation(LinearLayout.VERTICAL);nameBlock.addView(applianceText(ApplianceUsage.NAMES[i],14));indicators[i]=applianceText("",11);nameBlock.addView(indicators[i]);line.addView(nameBlock,new LinearLayout.LayoutParams(0,-2,1));
 powers[i]=applianceInput(old==null?applianceNumber(ApplianceUsage.WATTS[i]):old.optString("w",applianceNumber(ApplianceUsage.WATTS[i])),"Потужність: "+ApplianceUsage.NAMES[i]);
 hours[i]=applianceInput(old==null?applianceNumber(ApplianceUsage.HOURS[i]):old.optString("h",applianceNumber(ApplianceUsage.HOURS[i])),"Години на добу: "+ApplianceUsage.NAMES[i]);
 LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(dp(65),-2);wp.setMargins(dp(2),0,dp(2),0);line.addView(powers[i],wp);
 LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(65),-2);hp.setMargins(dp(2),0,0,0);line.addView(hours[i],hp);body.addView(line);
 View sep=new View(this);sep.setBackgroundColor(0xff34618b);body.addView(sep,new LinearLayout.LayoutParams(-1,dp(1)));
 }
 Runnable recalc=()->{
 double w=0,wh=0;boolean valid=true,start=false;
 for(int i=0;i<count;i++){double p=ApplianceUsage.parse(powers[i].getText().toString(),false),h=ApplianceUsage.parse(hours[i].getText().toString(),true);
 boolean good=positiveFinite(p)&&!Double.isNaN(h);
 powers[i].setError(positiveFinite(p)?null:"Вкажіть додатні Вт");hours[i].setError(!Double.isNaN(h)?null:"0–24 години");
 TextView indicator=indicators[i];
 if(!good){indicator.setText("Некоректні дані");indicator.setTextColor(0xffff7777);if(selected[i].isChecked())valid=false;}
 else if(!positiveFinite(limit)){indicator.setText("Межа невідома");indicator.setTextColor(0xffffdb70);}
 else if(p>limit){indicator.setText("Перевищено потужність");indicator.setTextColor(0xffff7777);}
 else if(ApplianceUsage.START_CHECK[i]||x.optInt("phases")!=1){indicator.setText(ApplianceUsage.START_CHECK[i]?"Перевірити пускове навантаження":"Перевірити фазність");indicator.setTextColor(0xffffdb70);}
 else{indicator.setText("У межах потужності окремо");indicator.setTextColor(0xff83e69a);}
 if(good&&selected[i].isChecked()){w+=p;wh+=ApplianceUsage.dailyWh(p,h);start|=ApplianceUsage.START_CHECK[i];}
 }
 boolean safe=valid&&positiveFinite(limit)&&w<=limit;
 loadValue.setText(valid?applianceNumber(w)+" Вт":"—");loadValue.setTextColor(!valid||positiveFinite(limit)&&w>limit?0xffff7777:positiveFinite(limit)?0xff83e69a:0xffffdb70);
 dailyValue.setText(valid?applianceNumber(wh/1000)+" кВт·год":"—");
 double runtime=safe&&w>0?ApplianceUsage.runtimeHours(energy,w):Double.NaN;
 runtimeValue.setText(ApplianceUsage.runtimeText(runtime));runtimeValue.setTextColor(positiveFinite(runtime)?0xff83e69a:Color.WHITE);
 double usable=ApplianceUsage.usableWh(energy),required=ApplianceUsage.dailyBatteryWh(wh),balance=ApplianceUsage.dailyBalanceWh(energy,wh);
 usableValue.setText(positiveFinite(usable)?applianceNumber(usable/1000)+" кВт·год":"—");
 requiredValue.setText(valid&&w>0?applianceNumber(required/1000)+" кВт·год":"—");
 boolean balanceKnown=valid&&w>0&&positiveFinite(usable)&&!Double.isNaN(balance);
 coverage.setVisibility(balanceKnown?View.VISIBLE:View.GONE);
 if(balanceKnown){coverage.setProgress((int)Math.round(Math.min(1,usable/required)*1000));balanceValue.setTextColor(balance<0?0xffff7777:0xff83e69a);
 balanceValue.setText(balance<0?"Енергії на добу недостатньо. Дефіцит: "+applianceNumber(-balance/1000)+" кВт·год":"За енергією на добу достатньо. Запас: "+applianceNumber(balance/1000)+" кВт·год");}
 else{balanceValue.setTextColor(0xffffdb70);balanceValue.setText("Добовий баланс не визначено.");}
 String message="";
 if(!valid)message="Виправте некоректні дані вибраних приладів.";
 else if(!positiveFinite(limit))message="Допустиме навантаження комплекту не підтверджено.";
 else if(w>limit)message="ПЕРЕВИЩЕНО РОЗРАХУНКОВЕ НАВАНТАЖЕННЯ!";
 else if(w==0)message="Оберіть прилади.";
 if(valid&&w>0&&start)message+=(message.isEmpty()?"":"\n")+"Перевірте пускове навантаження.";
 if(valid&&w>0&&x.optInt("phases")!=1)message+=(message.isEmpty()?"":"\n")+"Перевірте фазність і навантаження кожної фази.";
 warnings.setText(message);warnings.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);warnings.setTextColor(safe?0xffffdb70:0xffff7777);
 };
 android.text.TextWatcher watcher=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){recalc.run();}public void afterTextChanged(android.text.Editable e){}};
 for(int i=0;i<count;i++){powers[i].addTextChangedListener(watcher);hours[i].addTextChangedListener(watcher);selected[i].setOnCheckedChangeListener((v,c)->recalc.run());}
 body.addView(totals);pageText(body,"Оцінка: 90% ємності АКБ, ККД 90%, власне споживання інвертора 30 Вт протягом 24 годин. Без підзаряджання. Пускові ВА та фазність перевіряються окремо; це не гарантія сумісності.",13);
 LinearLayout footer=new LinearLayout(this);body.addView(footer);summaryButton(footer,"ЗБЕРЕГТИ",()->{try{JSONArray rows=new JSONArray();for(int i=0;i<count;i++){double p=ApplianceUsage.parse(powers[i].getText().toString(),false),h=ApplianceUsage.parse(hours[i].getText().toString(),true);if(!positiveFinite(p)||Double.isNaN(h)){Toast.makeText(this,"Виправте Вт та години (0–24)",Toast.LENGTH_LONG).show();return;}rows.put(new JSONObject().put("w",p).put("h",h).put("on",selected[i].isChecked()));}getPreferences(Context.MODE_PRIVATE).edit().putString(applianceKey(x,y),new JSONObject().put("rows",rows).toString()).apply();Toast.makeText(this,"Параметри збережено",Toast.LENGTH_SHORT).show();}catch(JSONException ex){Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();}});
 summaryButton(footer,"НАЗАД",d::dismiss);recalc.run();d.show();
 }
 void pairResult(JSONObject x,JSONObject y){showPreview(x,y);Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,"Вибраний енергокомплект");offerPhoto(body,x);pageText(body,offerLabel(x),20);offerLink(body,x);offerPhoto(body,y);pageText(body,offerLabel(y),20);offerLink(body,y);pageText(body,"Разом: "+totalPrice(x,y),24);pageText(body,pairCheck(x,y),17);pageButton(body,"ЕЛЕКТРОПРИЛАДИ ТА АВТОНОМНІСТЬ",()->applianceScreen(x,y));pageButton(body,"PDF / Друк",()->printPair(x,y));pageButton(body,"Оновити ціни",()->refreshPair(d,x,y));pageButton(body,"Ввести ціни вручну",()->manualPrices(d,x,y));pageButton(body,"Порівняти комплекти",()->compareKits(x,y));pageButton(body,"Зберегти комплект",()->{if(savePair(x,y)){d.dismiss();Toast.makeText(this,"Комплект збережено",Toast.LENGTH_SHORT).show();}});pageButton(body,"Зберегти та додати інший комплект",()->{if(savePair(x,y)){d.dismiss();for(EditText e:new EditText[]{inv,bat,ip,bp,iv,bv,ah,bms,watts})e.setText("");inverterOffer=null;batteryOffer=null;watts.requestFocus();}});pageButton(body,"Вихід",()->{if(savePair(x,y)){d.dismiss();finish();}});pageButton(body,"Назад",d::dismiss);d.show();}
 void compareKits(JSONObject x,JSONObject y){
 Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=page(d,"Порівняння комплектів");
 ArrayList<JSONObject> list=new ArrayList<>();for(int i=0;i<kits.length();i++){JSONObject k=kits.optJSONObject(i);if(k!=null)list.add(k);}
 if(x!=null&&y!=null){try{JSONObject k=new JSONObject();k.put("name",x.optString("model")+" + "+batteryName(y)+" (поточний)");k.put("price",x.optDouble("price")+y.optDouble("price"));k.put("energy",y.optDouble("voltage")*y.optDouble("ah")/1000);k.put("note",pairCheck(x,y));k.put("inverter_source",x.optString("seller")+" | "+x.optString("updated_at"));k.put("battery_source",y.optString("seller")+" | "+y.optString("updated_at"));list.add(k);}catch(JSONException ignored){}}
 Collections.sort(list,(a,b)->Double.compare(a.optDouble("price"),b.optDouble("price")));
 if(list.isEmpty())pageText(body,"Збережених комплектів немає",18);
 else{double cheapest=list.get(0).optDouble("price");pageText(body,"За зростанням збереженої ціни. Ціни не оновлюються автоматично.",16);int n=0;for(JSONObject k:list){pageText(body,"№"+(++n)+" "+k.optString("name"),20);pageText(body,String.format(java.util.Locale.US,"%.2f грн | +%.2f грн до мінімуму | %.2f кВт·год",k.optDouble("price"),k.optDouble("price")-cheapest,k.optDouble("energy")),18);pageText(body,k.optString("note")+"\nІнвертор: "+k.optString("inverter_source")+"\nАКБ: "+k.optString("battery_source"),16);}}
 pageButton(body,"Назад",d::dismiss);d.show();
 }
 void configureFeed(){EditText e=new EditText(this);e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);e.setHint("https://...");e.setText(getPreferences(Context.MODE_PRIVATE).getString("offer_feed",DEFAULT_FEED));new AlertDialog.Builder(this).setTitle("Джерело цін (JSON HTTPS)").setView(e).setPositiveButton("Зберегти",(d,w)->getPreferences(Context.MODE_PRIVATE).edit().putString("offer_feed",e.getText().toString().trim()).apply()).setNegativeButton("Скасувати",null).show();}
 void offers(boolean inverter){String endpoint=getPreferences(Context.MODE_PRIVATE).getString("offer_feed",DEFAULT_FEED);if(endpoint.isEmpty()){configureFeed();return;}try{double v=num(inverter?iv:bv),minimum=inverter?num(watts):num(ah);Toast.makeText(this,"Пошук пропозицій…",Toast.LENGTH_SHORT).show();new Thread(()->{try{java.util.List<JSONObject> list=OfferSource.load(endpoint,inverter?"inverter":"battery",v,minimum);runOnUiThread(()->{if(isFinishing())return;if(list.isEmpty()){new AlertDialog.Builder(this).setMessage("Пропозицій не знайдено").setPositiveButton("OK",null).show();return;}String[] labels=new String[list.size()];for(int i=0;i<labels.length;i++){JSONObject o=list.get(i);labels[i]=o.optString("model")+" — "+o.optDouble("price")+" грн\n"+o.optString("seller")+" | "+o.optString("updated_at");}new AlertDialog.Builder(this).setTitle("Пропозиції за ціною").setItems(labels,(d,i)->{JSONObject o=list.get(i);new AlertDialog.Builder(this).setTitle(o.optString("model")).setMessage(labels[i]+"\n"+o.optString("url")).setPositiveButton("Вибрати",(a,b)->{if(inverter)inverterOffer=o;else batteryOffer=o;(inverter?inv:bat).setText(o.optString("model"));(inverter?ip:bp).setText(o.optString("price"));(inverter?iv:bv).setText(o.optString("voltage"));(inverter?watts:ah).setText(o.optString(inverter?"kw":"ah"));if(!inverter){double verified=continuousCurrent(o);bms.setText(positiveFinite(verified)?String.valueOf(verified):"");}}).setNeutralButton("Магазин",(a,b)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(o.optString("url"))));}catch(Exception ignored){}}).setNegativeButton("Назад",null).show();}).show();});}catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Джерело цін недоступне",Toast.LENGTH_LONG).show());}}).start();}catch(Exception e){Toast.makeText(this,"Вкажіть напругу та потужність або ємність",Toast.LENGTH_SHORT).show();}}
 void search(String q){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(q))));}catch(Exception e){Toast.makeText(this,"Браузер недоступний",Toast.LENGTH_SHORT).show();}}
 void add(){try{double v=num(iv),a=num(bv),p=num(watts)*1000,current=num(bms);if(inv.getText().toString().trim().isEmpty()||bat.getText().toString().trim().isEmpty())throw new IllegalArgumentException();JSONObject k=new JSONObject();k.put("name",inv.getText()+" + "+bat.getText());k.put("price",num(ip)+num(bp));k.put("energy",a*num(ah)/1000);k.put("note",Math.abs(v-a)/v>0.1?"Напруга не відповідає":p>v*0.9*current*0.9?"BMS може обмежувати потужність":"Попередня оцінка струму достатня; перевірте паспорт і протоколи");k.put("inverter_source",source(inverterOffer,inv,ip));k.put("battery_source",source(batteryOffer,bat,bp));kits.put(k);save();render();}catch(Exception e){Toast.makeText(this,"Заповніть усі поля коректними значеннями",Toast.LENGTH_SHORT).show();}}
 String source(JSONObject o,EditText model,EditText price){if(o!=null && o.optString("model").equals(model.getText().toString()) && o.optDouble("price")==num(price))return o.optString("seller")+" | "+o.optString("updated_at")+" | "+o.optString("url");return "Ручне введення";}
 void save(){getPreferences(Context.MODE_PRIVATE).edit().putString("kits",kits.toString()).apply();}
 void render(){/* Збережені комплекти доступні в окремому вікні порівняння. */}
}