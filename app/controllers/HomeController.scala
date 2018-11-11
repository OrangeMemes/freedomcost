package controllers

import items.{Item, Pf1ItemProvider, Pf2ItemProvider}
import javax.inject._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.twirl.api.Html

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  private def itemProvider(gameName: String) = {
    Seq(Pf1ItemProvider, Pf2ItemProvider).find(_.GAME_NAME == gameName).get
  }

  def empty(gameName: String) = Action {
    showMessage(s"Воспользуйтесь поиском в верхней части экрана.", gameName)
  }

  def byId(id: String, gameName: String) = Action {
    val item = itemProvider(gameName).items.filter(_.id == id)
    showItemSeq(item, gameName)
  }

  def byName(name: String, gameName: String) = Action {
    val items = itemProvider(gameName).items.filter(_.title.toLowerCase.contains(name.toLowerCase))
    showItemSeq(items, gameName, name)
  }

  def uploadLogPage(gameName: String, password: String = "") = Action {
    constructPage(views.html.logupload(password), gameName)
  }

  case class LogForm(logdata: String, password: String)

  val logform = Form(
    mapping(
      "logdata" -> text,
      "password" -> text
    )(LogForm.apply)(LogForm.unapply)
  )

  def uploadLog(gameName: String) = Action { implicit request =>
    val data = logform.bindFromRequest.get
    if (!data.logdata.contains("Получено сообщение: 2301"))
      showMessage("Логи не содержат информации о дропе, их нужно переснять.", gameName)
    else if (data.password != "mister_orto")
      showMessage("Пароль неверный.", gameName)
    else
      showMessage(itemProvider(gameName).updateLogs(data.logdata), gameName)
  }

  private def showItemSeq(items: Seq[Item], gameName: String, searchString: String = "") = {
    if (items.nonEmpty)
      constructPage(views.html.list(items.map(views.html.listitem(_, gameName))), gameName, searchString)
    else {
      showMessage(s"Предмет с таким именем не найден или не поддерживается.", gameName)
    }
  }

  private def constructPage(content: Html, gameName: String, searchString: String = "") =
    Ok(views.html.page(content, itemProvider(gameName).modifiedTime, itemProvider(gameName).updatedTime, gameName, searchString, itemProvider(gameName).logUpdatedTime))


  private def showMessage(message: String, gameName: String) = {
    constructPage(views.html.text(message), gameName)
  }

  def update(gameName: String) = Action {
    itemProvider(gameName).updateItems()
    showMessage(s"Обновлено.", gameName)
  }

}
