import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mongodb.scala._

import scala.collection.JavaConverters._
import com.mongodb.client.model._
import com.datatech.sdp.mongo.engine._
import MGOClasses._

import scala.util._

object TestMongoEngine extends App {
  import MGOEngine._
  import MGOHelpers._
  import MGOCommands._
  import MGOAdmins._

  // or provide custom MongoClientSettings
  val settings: MongoClientSettings = MongoClientSettings.builder()
    .applyToClusterSettings(b => b.hosts(List(new ServerAddress("localhost")).asJava))
    .build()
  implicit val client: MongoClient = MongoClient(settings)

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
 // implicit val ec = system.dispatcher

  val ctx = MGOContext("testdb","po").setCommand(
    DropCollection("po"))

  import monix.execution.Scheduler.Implicits.global
  println(getResult(mgoAdmin(ctx).value.value.runToFuture))

scala.io.StdIn.readLine()

  val pic = fileToMGOBlob("/users/tiger/cert/MeTiger.png")
  val po1 = Document (
    "ponum" -> "po18012301",
    "vendor" -> "The smartphone compay",
    "podate" -> mgoDate(2017,5,13),
    "remarks" -> "urgent, rush order",
    "handler" -> pic,
    "podtl" -> Seq(
      Document("item" -> "sony smartphone", "price" -> 2389.00, "qty" -> 1239, "packing" -> "standard"),
      Document("item" -> "ericson smartphone", "price" -> 897.00, "qty" -> 1000, "payterm" -> "30 days")
    )
  )

  val po2 = Document (
    "ponum" -> "po18022002",
    "vendor" -> "The Samsung compay",
    "podate" -> mgoDate(2015,11,6),
    "podtl" -> Seq(
      Document("item" -> "samsung galaxy s8", "price" -> 2300.00, "qty" -> 100, "packing" -> "standard"),
      Document("item" -> "samsung galaxy s7", "price" -> 1897.00, "qty" -> 1000, "payterm" -> "30 days"),
      Document("item" -> "apple iphone7", "price" -> 6500.00, "qty" -> 100, "packing" -> "luxury")
    )
  )

  val optInsert = new InsertManyOptions().ordered(true)
  val ctxInsert = ctx.setCommand(
    Insert(Seq(po1,po2),Some(optInsert))
  )
  println(getResult(mgoUpdate(ctxInsert).value.value.runToFuture))

  scala.io.StdIn.readLine()

  case class PO (
                  ponum: String,
                  podate: MGODate,
                  vendor: String,
                  remarks: Option[String],
                  podtl: Option[MGOArray],
                  handler: Option[MGOBlob]
                )
  def toPO(doc: Document): PO = {
    PO(
      ponum = doc.getString("ponum"),
      podate = doc.getDate("podate"),
      vendor = doc.getString("vendor"),
      remarks = mgoGetStringOrNone(doc,"remarks"),
      podtl = mgoGetArrayOrNone(doc,"podtl"),
      handler = mgoGetBlobOrNone(doc,"handler")
    )
  }

  case class PODTL(
                    item: String,
                    price: Double,
                    qty: Int,
                    packing: Option[String],
                    payTerm: Option[String]
                  )
  def toPODTL(podtl: Document): PODTL = {
    PODTL(
      item = podtl.getString("item"),
      price = podtl.getDouble("price"),
      qty = podtl.getInteger("qty"),
      packing = mgoGetStringOrNone(podtl,"packing"),
      payTerm = mgoGetStringOrNone(podtl,"payterm")
    )
  }

  def showPO(po: PO) = {
    println(s"po number: ${po.ponum}")
    println(s"po date: ${mgoDateToString(po.podate,"yyyy-MM-dd")}")
    println(s"vendor: ${po.vendor}")
    if (po.remarks != None)
      println(s"remarks: ${po.remarks.get}")
    po.podtl match {
      case Some(barr) =>
        mgoArrayToDocumentList(barr)
          .map { dc => toPODTL(dc)}
          .foreach { doc: PODTL =>
            print(s"==>Item: ${doc.item} ")
            print(s"price: ${doc.price} ")
            print(s"qty: ${doc.qty} ")
            doc.packing.foreach(pk => print(s"packing: ${pk} "))
            doc.payTerm.foreach(pt => print(s"payTerm: ${pt} "))
            println("")
          }
      case _ =>
    }

    po.handler match {
      case Some(blob) =>
        val fileName = s"/users/tiger/${po.ponum}.png"
        mgoBlobToFile(blob,fileName)
        println(s"picture saved to ${fileName}")
      case None => println("no picture provided")
    }

  }

  import org.mongodb.scala.model.Projections._
  import org.mongodb.scala.model.Filters._
  import org.mongodb.scala.model.Sorts._
  import org.mongodb.scala.bson.conversions._
  import org.mongodb.scala.bson.Document


  val ctxFilter = Find(filter=Some(equal("podtl.qty",100)))


  val sort: Bson = (descending("ponum"))
  val proj: Bson = (and(include("ponum","podate")
                   ,include("vendor"),excludeId()))
  val resSort = ResultOptions(FOD_TYPE.FOD_SORT,Some(sort))
  val resProj = ResultOptions(FOD_TYPE.FOD_PROJECTION,Some(proj))
  val ctxFind = ctx.setCommand(Find(andThen=Seq(resProj,resSort)))

  val ctxFindFirst = ctx.setCommand(Find(firstOnly=true))
  val ctxFindArrayItem = ctx.setCommand(
    Find(filter = Some(equal("podtl.qty",100)))
  )

  for {
    _ <- mgoQuery[List[Document]](ctxFind).value.value.runToFuture.andThen {
      case Success(eold) => eold match {
        case Right(old) => old match {
          case Some(ld) => ld.map(toPO(_)).foreach(showPO)
          case None => println(s"Empty document found!")
        }
        case Left(err) => println(s"Error: ${err.getMessage}")
      }
        println("-------------------------------")
      case Failure(e) => println(e.getMessage)
    }

    _ <- mgoQuery[PO](ctxFindFirst,Some(toPO _)).value.value.runToFuture.andThen {
      case Success(eop) => eop match {
        case Right(op) => op match {
          case Some(p) => showPO(_)
          case None => println(s"Empty document found!")
        }
        case Left(err) => println(s"Error: ${err.getMessage}")
      }
        println("-------------------------------")
      case Failure(e) => println(e.getMessage)
    }

    _ <- mgoQuery[List[PO]](ctxFindArrayItem,Some(toPO _)).value.value.runToFuture.andThen {
      case Success(eops) => eops match {
        case Right(ops) => ops match {
          case Some(lp) => lp.foreach(showPO)
          case None => println(s"Empty document found!")
        }
        case Left(err) => println(s"Error: ${err.getMessage}")
      }
        println("-------------------------------")
      case Failure(e) => println(e.getMessage)
    }
  } yield()


  scala.io.StdIn.readLine()


  system.terminate()
}
