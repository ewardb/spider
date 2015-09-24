import javax.swing.text.html.HTML

import WB.WEBEngine
import org.jsoup.select.Elements
import org.jsoup.{Jsoup}
import org.jsoup.nodes.{Element, Document}
import scala.collection.convert.wrapAsScala._

import scala.collection.mutable.ListBuffer

/**
 * Created by isaac on 15/9/24.
 */
package object spider {
  def main(args: Array[String]) {
    val url_list = List("", "news_hot/", "news_society/", "news_entertainment/", "news_tech/", "news_car/", "news_sports/")
    val content_list = ListBuffer[Map[String,AnyRef]]()
    val arr = new Array[String](3)
       //url_list.foreach(i => WEBEngine.getContentFromUrl("http://toutiao.com/" + i,1))

      url_list.map(i=>"http://toutiao.com/"+i).map( WEBEngine.getContentFromUrl(_, 1)).map { s =>
      val doc: Document = Jsoup.parse(s)
      val eles:Elements = doc.getElementById("pagelet-feedlist").getElementsByTag("ul").toArray().head.asInstanceOf[Element].getElementsByClass("item")
      eles.foreach {ele=>
        val href:String = ele.getElementsByClass("title").toArray.head.asInstanceOf[Element].attr("href")
        val title:String = ele.getElementsByClass("title").toArray.head.asInstanceOf[Element].getElementsByTag("a").toArray().head.asInstanceOf[Element].text()
        ele.getElementsByClass("feed-img").toArray().map{s =>
          val src = s.asInstanceOf[Element].attr("src")
          arr.+(src)
        }
        val like:String = ele.getElementsByClass("liked-num").toArray().head.asInstanceOf[Element].text()
        val disLike:String = ele.getElementsByClass("disliked-num").toArray().head.asInstanceOf[Element].text()
        val from:String = ele.getElementsByClass("btn").toArray().head.asInstanceOf[Element].text()
        val date:String = ele.getElementsByClass("datetime").toArray().head.asInstanceOf[Element].text()
        content_list.append(Map("href"->href,"title"->title,"arr"->arr,"like"->like,"disLike"->disLike,"from"->from,"date"->date))
      }

    }

  }
}
