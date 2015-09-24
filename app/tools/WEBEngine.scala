package WB

import java.io.ByteArrayInputStream
import java.net.URL
import javax.imageio.ImageIO

//import DB._
import _root_.Tool.Tool._
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.html.HtmlPage
import Tool.Tool._
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.remote.DesiredCapabilities
import tools.{Aliyun, NetTool}
import com.gargoylesoftware.htmlunit._
import tools.{NetTool, Aliyun}
import scala.collection.convert.WrapAsScala._

/**
 * Created by zixuan on 15/8/27.
 */
object WEBEngine {
  def fs=getSettingCacheObject("aliyunId", "aliyunKey","aliyunBucket","aliyunEndpoint","aliyunPublicEndpoint")(list => new Aliyun(list(0), list(1),list(2),list(3),list(4)))

  lazy private val unitDriver = {

    val webClient= new WebClient(BrowserVersion.FIREFOX_17)
    webClient.getOptions().setCssEnabled(false)
    webClient.getOptions().setJavaScriptEnabled(true)
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false)
    webClient.getOptions().setThrowExceptionOnScriptError(false)
    //    webClient.getOptions().setProxyConfig(new ProxyConfig("127.0.0.1",8888))
    webClient.getCookieManager().setCookiesEnabled(true)
    webClient.setAjaxController(new NicelyResynchronizingAjaxController())
    webClient.waitForBackgroundJavaScript(2*1000)
    webClient.addRequestHeader("User-Agent",ua)

    //    val hud=new HtmlUnitDriver(new BrowserVersion(
    //      "Firefox", "5.0", ua, 6 //important is 3rd argument
    //    ))
    //    hud.setJavascriptEnabled(true)
    //    hud
    webClient
  }
  val ua="Mozilla/5.0 (Linux; Android 5.0; SM-N9100 Build/LRX21V) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36 MicroMessenger/6.0.2.56_r958800.520 NetType/WIFI"

  lazy private val phantomDriver ={
    val desiredCapabilities = DesiredCapabilities.phantomjs()
    desiredCapabilities.setCapability("loadImages",false)
    desiredCapabilities.setCapability("phantomjs.page.settings.userAgent",ua)
    new PhantomJSDriver(desiredCapabilities)
  }

  private def getContentFromHtmlUnit(url:String)={
    val page:HtmlPage=unitDriver.getPage(url)
    Thread.sleep(1000)
    page.asXml()
  }

  private def getContentFromPhantomJ(url:String)={
    phantomDriver.get(url)
    val ebElement = phantomDriver.findElement(By.xpath("/html"))
    ebElement.getAttribute("outerHTML")
  }

  private def getContentFromHttpClient(url:String)={
    NetTool.HttpGet(url,appendHead = Map("User-Agent"->ua))._2
  }

  def getContentFromUrl(url:String,mockType:Int)={
    mockType match{
      case 0 => getContentFromHttpClient(url)
      case 1=>getContentFromHtmlUnit(url)
      case 2 => getContentFromPhantomJ(url)
      case _ => getContentFromPhantomJ(url)
    }
  }

  def replaceImageUrl(url:String,ad:String)={
    val imgData=NetTool.HttpGetBin(url.replace("&tp=webp",""),appendHead =  Map("User-Agent"->ua))._2
    try{
      (true,ad)
    } catch {case e: Throwable =>
      val picid=System.currentTimeMillis().toString+".jpeg"
      fs.saveFile(picid,imgData,"image/jpeg")
      (false,fs.getFileUrl(picid.toString()))
    }
  }
}
