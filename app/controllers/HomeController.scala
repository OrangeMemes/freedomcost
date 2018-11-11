package controllers

import items.{Item, Pf1ItemProvider, Pf2ItemProvider}
import javax.inject._
import play.api.mvc._

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

  private def showItemSeq(items: Seq[Item], gameName: String, searchString: String = "") = {
    if (items.nonEmpty)
      Ok(views.html.page(views.html.list(items.map(views.html.listitem(_, gameName))), itemProvider(gameName).modifiedTime, itemProvider(gameName).updatedTime, gameName, searchString))
    else {
      showMessage(s"Предмет с таким именем не найден или не поддерживается.", gameName)
    }
  }

  private def showMessage(message: String, gameName: String) = {
    Ok(views.html.page(views.html.text(message), itemProvider(gameName).modifiedTime, itemProvider(gameName).updatedTime, gameName))
  }

  def update(gameName: String) = Action {
    itemProvider(gameName).updateItems()
    showMessage(s"Обновлено.", gameName)
  }

}
