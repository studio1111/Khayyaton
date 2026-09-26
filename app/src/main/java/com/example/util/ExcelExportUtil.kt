package com.example.util
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.FurnitureOrder
import com.example.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExportUtil {
 private fun esc(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
 private fun col(i0:Int):String{var i=i0+1;val s=StringBuilder();while(i>0){s.append(('A'.code+(i-1)%26).toChar());i=(i-1)/26};return s.reverse().toString()}
 private fun cell(r:String,v:String,st:Int=0)="<c r=\"" + r + "\" t=\"inlineStr\" s=\"" + st + "\"><is><t>" + esc(v) + "</t></is></c>"
 private fun num(r:String,v:Long)="<c r=\"" + r + "\" t=\"n\" s=\"2\"><v>" + v + "</v></c>"
 private fun sheet(h:List<String>,rows:List<List<String>>,nums:Set<Int>):String{
  val safeRows=if(rows.isEmpty()) listOf(List(h.size){i->if(i==0)"داده‌ای برای نمایش ثبت نشده است" else ""}) else rows
  val all=listOf(h)+safeRows
  val data=buildString{all.forEachIndexed{ri,row->append("<row r=\"" + (ri+1) + "\">");row.forEachIndexed{ci,v->val ref=col(ci)+(ri+1);if(ri>0&&ci in nums){val n=v.replace(",","").replace("٬","").replace(" ","").toLongOrNull();append(if(n!=null)num(ref,n) else cell(ref,v))}else append(cell(ref,v,if(ri==0)1 else 0))};append("</row>")}}
  val widths=h.indices.joinToString(""){i->val m=(listOf(h[i])+safeRows.mapNotNull{it.getOrNull(i)}).maxOfOrNull{it.length}?:10;"<col min=\"" +(i+1)+"\" max=\"" +(i+1)+"\" width=\"" +(m.coerceIn(10,30)+3)+"\"/>"}
  return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" state=\"frozen\"/></sheetView></sheetViews><cols>"+widths+"</cols><sheetData>"+data+"</sheetData><autoFilter ref=\"A1:"+col(h.lastIndex)+all.size+"\"/></worksheet>"
 }
 private fun wb(names:List<String>)="<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+names.mapIndexed{i,n->"<sheet name=\"" + esc(n) + "\" sheetId=\"" +(i+1)+"\" r:id=\"rId"+(i+1)+"\"/>"}.joinToString("")+"</sheets></workbook>"
 private val styles="<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0\"/></numFmts><fonts count=\"2\"><font><sz val=\"11\"/></font><font><b/><sz val=\"11\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE2E8F0\"/></patternFill></fill></fills><borders count=\"2\"><border><left/><right/><top/><bottom/></border><border><left style=\"thin\"/><right style=\"thin\"/><top style=\"thin\"/><bottom style=\"thin\"/></border></borders><cellXfs count=\"3\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"1\" borderId=\"1\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\"/></cellXfs></styleSheet>"
 private fun write(f:File,s:List<Pair<String,String>>){
  ZipOutputStream(FileOutputStream(f)).use{z->
   fun put(p:String,x:String){z.putNextEntry(ZipEntry(p));z.write(x.toByteArray(Charsets.UTF_8));z.closeEntry()}
   put("[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"+s.indices.joinToString(""){"<Override PartName=\"/xl/worksheets/sheet"+(it+1)+".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>")
   put("_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
   put("xl/workbook.xml",wb(s.map{it.first}))
   put("xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+s.indices.joinToString(""){"<Relationship Id=\"rId"+(it+1)+"\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet"+(it+1)+".xml\"/>"}+"<Relationship Id=\"rId"+(s.size+1)+"\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>")
   put("xl/styles.xml",styles);s.forEachIndexed{i,p->put("xl/worksheets/sheet"+(i+1)+".xml",p.second)}
  }
 }
 fun shareInvoiceExcel(context:Context,orders:List<FurnitureOrder>,payments:List<PaymentRecord>,currencyUnit:String){
  val f=File(context.cacheDir,"khayyaton_invoice_"+System.currentTimeMillis()+".xlsx")
  val oh=listOf("ردیف","تاریخ","شماره فاکتور","مدل مبل","واحد","دستمزد کل ("+currencyUnit+")","طرف حساب")
  val or=orders.mapIndexed{i,x->listOf((i+1).toString(),x.dateJalali,x.invoiceNumber,x.modelName,x.calculatedUnits.toString(),x.calculatedTotal.toString(),x.customerName)}
  val ph=listOf("ردیف","تاریخ","طرف حساب","مبلغ دریافتی ("+currencyUnit+")","نوع پرداخت","شماره پیگیری","شرح")
  val pr=payments.mapIndexed{i,x->listOf((i+1).toString(),x.dateJalali,x.customerName,x.amount.toString(),x.paymentType,x.referenceNo,x.description)}
  write(f,listOf("کارکرد" to sheet(oh,or,setOf(0,4,5)),"دریافتی‌ها" to sheet(ph,pr,setOf(0,3))));share(context,f,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","خروجی اکسل خیاطان")
 }
 fun shareAnalysisExcel(context:Context,orders:List<FurnitureOrder>,payments:List<PaymentRecord>,currencyUnit:String){
  val f=File(context.cacheDir,"khayyaton_analysis_"+System.currentTimeMillis()+".xlsx")
  val oh=listOf("ردیف","تاریخ","مدل مبل","واحد","دستمزد ("+currencyUnit+")","طرف حساب")
  val or=orders.mapIndexed{i,x->listOf((i+1).toString(),x.dateJalali,x.modelName,x.calculatedUnits.toString(),x.calculatedTotal.toString(),x.customerName)}
  val ph=listOf("ردیف","تاریخ","طرف حساب","دریافتی ("+currencyUnit+")","نوع","پیگیری")
  val pr=payments.mapIndexed{i,x->listOf((i+1).toString(),x.dateJalali,x.customerName,x.amount.toString(),x.paymentType,x.referenceNo)}
  write(f,listOf("تحلیل کارگاه" to sheet(oh,or,setOf(0,3,4)),"دریافتی‌ها" to sheet(ph,pr,setOf(0,3))));share(context,f,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","خروجی اکسل آنالیز کارگاه")
 }
 private fun share(c:Context,f:File,m:String,t:String){val u=FileProvider.getUriForFile(c,c.packageName+".fileprovider",f);val i=Intent(Intent.ACTION_SEND).apply{type=m;putExtra(Intent.EXTRA_STREAM,u);putExtra(Intent.EXTRA_SUBJECT,t);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)};c.startActivity(Intent.createChooser(i,t).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
}
