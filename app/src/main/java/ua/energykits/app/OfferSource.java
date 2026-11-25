package ua.energykits.app;
import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
final class OfferSource {
 static List<JSONObject> load(String endpoint,String kind,double voltage,double minimum) throws Exception {
  return matches(load(endpoint), kind, voltage, minimum);
 }
 static JSONArray load(String endpoint) throws IOException, JSONException {
  URL url=new URL(endpoint);
  if(!"https".equalsIgnoreCase(url.getProtocol())) throw new IOException("HTTPS required");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();
  c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(false);
  c.setRequestProperty("Accept","application/json");
  try {
   if(c.getResponseCode()!=200) throw new IOException("HTTP "+c.getResponseCode());
   try(InputStream input=c.getInputStream()){return read(input);}
  }finally{c.disconnect();}
 }
 static JSONArray read(InputStream input) throws IOException, JSONException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  byte[] buffer=new byte[4096];
  int count;
  while((count=input.read(buffer))!=-1){
   if(out.size()+count>2000000)throw new IOException("Feed too large");
   out.write(buffer,0,count);
  }
  return new JSONArray(out.toString("UTF-8"));
 }
 static List<JSONObject> matches(JSONArray data,String kind,double voltage,double minimum){
   List<JSONObject> matches=new ArrayList<>();
   for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;
    double price=o.optDouble("price"),v=o.optDouble("voltage"),rating=o.optDouble("inverter".equals(kind)?"kw":"ah");
    if(!"out_of_stock".equals(o.optString("availability"))&&!"trade_in".equals(o.optString("condition"))&&kind.equals(o.optString("kind"))&&"UAH".equals(o.optString("currency"))&&price>0&&!Double.isInfinite(price)&&v>0&&!Double.isInfinite(v)&&(voltage==0||Math.abs(v-voltage)<=voltage*0.1)&&rating>=minimum&&!Double.isInfinite(rating)&&!o.optString("model").isEmpty()&&!o.optString("seller").isEmpty()&&!o.optString("updated_at").isEmpty()&&o.optString("url").startsWith("https://"))matches.add(o);
   }
   Collections.sort(matches,(a,b)->Double.compare(a.optDouble("price"),b.optDouble("price")));return matches;
 }
}
