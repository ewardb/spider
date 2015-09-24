package Tool

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.Executors
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.collection.Parallelizable
import scala.collection.parallel.{ForkJoinTaskSupport, ParIterable}
import scala.concurrent._
import scala.concurrent.forkjoin.ForkJoinPool
import scala.util.Random

/**
 * Created by 林 on 14-4-3.
 */
object Tool {

  private val chars: Array[Char] = "0123456789ABCDEF".toCharArray
  private val settingObjectCache = new SoftHashMap[String, AnyRef](20)
  private val cache = new SoftHashMap[String, (Long, AnyRef)](20)
  private val AES_DEFAULT_KEY = "#$%^Setgd4g$%^"
  private val map = new ObjectMapper() with ScalaObjectMapper
  map.registerModule(DefaultScalaModule)
  map.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)

  val pool = Executors.newFixedThreadPool(100)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

  /*
   * 霸气侧漏吊炸天的东西,把集合方法直接变成多线程执行
   */
  implicit class ParToMutile[+A](parable: Parallelizable[A, ParIterable[A]]) {
    def mutile(thread: Int = -1) = {
      if (thread == -1) {
        parable.par
      } else {
        val resutl = parable.par
        resutl.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(thread))
        resutl
      }
    }
  }

  implicit class AnyRefAddMethod[A <: AnyRef](bean: A) {
    def toJson(): String = {
      map.writeValueAsString(bean)
    }

    def toBean(json: String): A = {
      Tool.toBean(json, bean.getClass)
    }
//
//    def checkEmpty(): A = {
//      bean match {
//        case _: String => if (bean.asInstanceOf[String].trim.isEmpty) new EmptyFieldExcepiton()
//        case _: List[_] => if (bean.asInstanceOf[List[AnyRef]].isEmpty) new EmptyFieldExcepiton()
//        case _: Map[_, _] => if (bean.asInstanceOf[Map[AnyRef, AnyRef]].isEmpty) new EmptyFieldExcepiton()
//        case _: Array[_] => if (bean.asInstanceOf[Array[AnyRef]].length == 0) new EmptyFieldExcepiton()
//        case _ => if (bean == null) new EmptyFieldExcepiton()
//      }
//      bean
//    }

  }

  def toBean[T](json: String, clazz: Class[T]): T = {
    map.readValue(json, clazz).asInstanceOf[T]
  }


  def isAESData(s: String) = {
    s.length % 32 == 0 && s.matches("[0-9a-fA-F]+")
  }


  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
    }
  }

  /**
   * md5加密.
   *
   * @param str
	 * the str
   * @return string
   * @throws Exception
	 * the exception
   */
  def md5(str: String): String = {
    val md5: MessageDigest = MessageDigest.getInstance("MD5")
    val sb: StringBuilder = new StringBuilder
    for (b <- md5.digest(str.getBytes("utf-8"))) {
      sb.append(str2HexStr(b))
    }
    return sb.toString
  }

  /**
   * Str to hex str.
   *
   * @param b the b
   * @return the string
   * @author 黄林
   */
  def str2HexStr(b: Byte): String = {
    val r: Array[Char] = new Array[Char](2)
    var bit: Int = (b & 0x0f0) >> 4
    r(0) = chars(bit)
    bit = b & 0x0f
    r(1) = chars(bit)
    val str: String = new String(r)
    return str
  }

  //带重试的区段
  def reTry(count: Int = 5)(f: => Unit) {
    var retryCount = 0
    while (retryCount <= count) {
      try {
        f
        retryCount = count + 1
      } catch {
        case e: Throwable =>
          retryCount += 1
          Thread.sleep(10);
          if (retryCount > count) {
            throw e
          }
      }
    }
  }

  def safe[T](f: => T) = {
    try {
      f
    } catch {
      case e: Throwable =>
//        e.printStackTrace()
        null.asInstanceOf[T]
    }
  }

  /**
   * 获取表达式的内容
   *
   * @param context
	 * 内容
   * @param separate
	 * 表达式分隔
   * @return the expr
   */
  def getExpr(context: String, separate: String): Option[String] = {
    getExpr(context, separate, separate, null)
  }

  /**
   * 获取表达式的内容.
   *
   * @param context
	 * 内容
   * @param startseparate
	 * 表达式分隔开始
   * @param endSeparate
	 * 表达式分隔结束
   * @param includeKey
	 * 表达式关键字
   * @return the expr
   */
  def getExpr(context: String, startseparate: String,
              endSeparate: String, includeKey: String): Option[String] = {
    if (null == context || context.isEmpty || context.indexOf(startseparate) == -1) {
      None
    } else {
      val start = context.indexOf(startseparate)
      val end = context.indexOf(endSeparate, start + 1)
      val result = context.substring(start, end + 1)
      if (null != includeKey && result.indexOf(includeKey) == -1) {
        getExpr(context.drop(end), startseparate, endSeparate, includeKey)
      } else
        Some(result)
    }
  }

  def getSettingMap(): Map[String, String] = {Map("a"->"a")}
    //这里取数据有一秒缓存延迟
    //getJedisCacheMap(SETTING_VALUE_CACHE_KEY)


//  def getJedisCacheMap(key: String): Map[String, String] = {
//    cacheMethod("get_redis_" + key + "_map", 1) {
//      JedisTool.useJedis(_.hgetall(key))
//    }.asInstanceOf[Map[String, String]]
//  }

  def getSettingCacheObject[T](keys: String*)(f: Array[String] => T): T = {
    val setting = getSettingMap()
    val key = keys.mkString
    val values = settingObjectCache.get(key + "_values").asInstanceOf[Map[String, String]]
    if (null != values && null != settingObjectCache.get(key)) {
      val hasNoChange = keys map (k => setting.get(k).equals(values(k))) reduceLeft (_ && _)
      if (hasNoChange) {
        return settingObjectCache.get(key).asInstanceOf[T]
      }
    }
    val lists = keys map (setting.get(_).get)
    settingObjectCache.put(key + "_values", (keys zip lists toMap))
    f(lists toArray)
  }

  /*
  仅用于本地jvm的缓存方法
   */
  def cacheMethod[T <: AnyRef](key: String, time: Float)(f: => T): T = {
    val now = System.currentTimeMillis() / 1000
    val value = cache.get(key)
    if (value != null && value._1 > (now - time)) {
      value._2.asInstanceOf[T]
    } else {
      val v = f
      cache.put(key, (now, v))
      v
    }
  }
  def setCache(key: String, v: AnyRef,time:Int) = {
    val now = System.currentTimeMillis() / 1000
    cache.put(key, (now+ time, v ))
  }
  def getCache(key: String) = {
    val now = System.currentTimeMillis() / 1000
    val value = cache.get(key)
    if (value != null && value._1 > now) {
      Some(cache.get(key)._2)
    } else {
      if (value != null && value._1 <= now) cache.remove(key)
      None
    }
  }
  def delCache(key: String) = {
    cache.remove(key)
  }

  //后台执行
  def run[T](body: => T) = Future[T](body)


  implicit class StringAddMethod[A <: String](bean: A) {
//    def encrypt(): String = {
//      AESCoder.encrypt(bean, AES_DEFAULT_KEY)
//    }
//
//    def decrypt(): String = {
//      AESCoder.decrypt(bean, AES_DEFAULT_KEY)
//    }

    def md5(): String = Tool.md5(bean)

    def isPhone: Boolean = """^1\d{10}$""".r.pattern.matcher(bean).matches()

    def isNumber: Boolean = """^\d+$""".r.pattern.matcher(bean).matches()

    def toBigDecimal = if (isEmpty(bean)) null else BigDecimal(bean)

    def safeInt(v:Int= -1) = if (isEmpty(bean)) v else bean.toInt

    def safeInt:Int=safeInt(-1)

    def safeDouble = if (isEmpty(bean)) -1d else bean.toDouble

    def safeLong = if (isEmpty(bean)) -1l else bean.toLong

    def toIntList(split:String) = StrtoList[Int](bean, split, _.toInt)

    def toIntList = StrtoList[Int](bean, ",", _.toInt)

    def toLongList = StrtoList[Long](bean, ",", _.toLong)

    def toDoubleList = StrtoList[Double](bean, ",", _.toDouble)
  }

//  implicit class DateAddMethod[A <: Date](bean: A) {
//    //yy-mm-dd
//    def sdate = if (bean == null) "" else TimeTool.getDateStringByDate(bean)
//
//    //yy-mm-dd
//    def sdatetime = if (bean == null) "" else TimeTool.getFormatStringByDate(bean)
//  }

  implicit class NumberAddMethod[A <: BigDecimal](bean: A) {
    def toMoney(): BigDecimal = {
      bean.setScale(2, BigDecimal.RoundingMode.HALF_UP)
    }
  }

  implicit class IntegerAddMethod[A <: Int](bean: A) {
    def checkStr = if (isEmpty(bean)) "" else bean.toString
  }

  //数字自动转字符串  (老子受够了到处写toString)
  implicit def intToString(i: Int): String = i.toString

  def isEmpty(str: String) = {
    (null == str || str.isEmpty)
  }

  def isEmpty(bean: Any): Boolean = {
    bean match {
      case s: String => isEmpty(bean.asInstanceOf[String])
      case i: Int => bean.asInstanceOf[Int] == -1
      case d: Double => bean.asInstanceOf[Double] == -1
      case b: BigDecimal => b == null || b.asInstanceOf[BigDecimal] == -1
      case a: Traversable[_] => a == null || a.asInstanceOf[Traversable[AnyRef]].isEmpty
      case _ => bean == null
    }
  }

  def StrtoList[T](bean: String, split: String, fun: String => T): List[T] = {
    if (isEmpty(bean)) Nil else bean.split(split).map(fun(_)).toList
  }

  def randomStr(len: Int) = {
    val randomValue = randomChars + randomNums
    0 to (len - 1) map (v => randomValue(Random.nextInt(randomValue.length))) mkString
  }

  private val randomChars = "abcdefghjkmnpqrstvwxyABCDEFGHJKLMNPQRSTVWXY2346789"
  private val randomNums = "2346789"

    /*
  全服务器防冲突任务
   */
//  def onlyTask(name:String)(f: => Unit) {
//    val task=name+"_task"
//    try {
//      //3毫秒内的延迟，保证任务都能错开执行
//      Thread.sleep(3)
//      if(JedisTool.useJedis(_.get(task)).isEmpty) {
//        JedisTool.useJedis(_.set(task, 1))
//        f
//      }
//    } catch {
//      case e: Throwable =>
//        e.printStackTrace()
//    }finally {
//      JedisTool.useJedis(_.del(task))
//    }
//  }
  def gzip(data:Array[Byte])={
    val bos = new ByteArrayOutputStream()
    val gzip = new GZIPOutputStream(bos)
    gzip.write(data)
    gzip.finish()
    gzip.close()
    val gdata = bos.toByteArray()
    bos.close()
    gdata
  }
  def ungzip(gdata:Array[Byte])={
    val bis = new ByteArrayInputStream(gdata)
    val gzip = new GZIPInputStream(bis)
    val buf = new Array[Byte](1024)
    var num = -1
    val  baos = new ByteArrayOutputStream()
    num = gzip.read(buf, 0, buf.length)
    while (num != -1) {
      baos.write(buf, 0, num)
      num = gzip.read(buf, 0, buf.length)
    }
    val data = baos.toByteArray()
    baos.flush()
    baos.close()
    gzip.close()
    bis.close()
    data
  }
  implicit class DataAddMethod[A <: Array[Byte]](data: A) {
    def gzip = Tool.gzip(data)
    def ungzip=Tool.ungzip(data)
  }

}
