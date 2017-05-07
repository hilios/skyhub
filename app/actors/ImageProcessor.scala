package actors

import javax.inject._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import dao.ImagesDAO
import models.Image
import services.ImagesService

import scala.concurrent.duration._

class ImageProcessor @Inject()(images: ImagesService, imagesDAO: ImagesDAO) extends Actor {
  import ImageProcessor._
  import Thumbinator._
  import context.dispatcher

  val thumb = context.actorOf(Thumbinator.props, "thumbinator")
  implicit val timeout: Timeout = 5.minutes

  def receive = {
    case Fetch(url) =>
      println(s"Processing $url")
      for {
        _ <- imagesDAO.findByUrl(url)
        image <- images.load(url)
        sm <- (thumb ? Small(image)).mapTo[Array[Byte]]
        lg <- (thumb ? Large(image)).mapTo[Array[Byte]]
        md <- (thumb ? Medium(image)).mapTo[Array[Byte]]
      } yield {
        println(s"Inserting $url")
        val img = Image(url, sm, md, lg)
        imagesDAO.insert(img)
      }
  }
}

object ImageProcessor {
  def props = Props[ImageProcessor]

  case class Fetch(url: String)
}
