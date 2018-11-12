package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}
import play.twirl.api.Html

@Singleton
class IndexController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action {
    Ok(Html(
      s"""
         |<head>
         |<link rel="stylesheet" href="${routes.Assets.versioned("stylesheets/main.css")}">
         |</head>
         |<body>
         |<a href="${routes.HomeController.empty("fc1")}" class="button">Перейти к ЦС1</a>
         |<a href="${routes.HomeController.empty("fc2")}" class="button">Перейти к ЦС2</a>
         |</body>
      """.stripMargin))
  }
}
