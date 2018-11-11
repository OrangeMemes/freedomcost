package items

case class Room(id: String, modes: Seq[Mode], rarity: Rarity) {
  def getText(isPf2: Boolean = false): String = id match {
    case "702" => "Кухня"
    case "701" => "Гостиная"
    case "703" => "Подвал"
    case "704" => "Прихожая"
    case "705" => "Детская"
    case "706" => "Библиотека"
    case "707" => "Чердак"
    case "708" => "Спальня"
    case "709" => "Зимний сад"
    case "710" => "Кинотеатр"
    case "711" => "Кабинет"
    case "712" => "Логово"
    case "721" => "Ванная"
    case "722" => "Индийская"
    case "723" => "Гараж"
    case "724" => "Обсерватория"
    case "725" => "Лоджия"
    case "726" => "Заброшенная комната"
    case "713" => "Фотолаборатория"
    case "714" => "Ванная"
    case "727" => "Японская комната"
    case "728" => "Погреб"
    case "729" => "Музыкальная студия"
    case "730" => if (isPf2) "Сторожка" else "Праздничная комната"
    case "731" => "Сторожка"
    case "732" => "Детская площадка"
    case "733" => "Мерцающее озеро"
    case "734" => "Беседка"
    case unknown => s"Неизвестная комната ($unknown)"
  }
}

case class Mode(id: Int) {
  def getName: String = id match {
    case 1 => "Слова"
    case 2 => "Тени"
    case 3 => "Ночь"
    case 4 => "Тайник"
    case 5 => "Призраки"
    case 6 => "Тьма"
    case 7 => "Найди пару"
    case 8 => "Анаграммы"
    case 9 => "Строгий порядок"
    case 11 => "Невидимые чернила"
    case 12 => "Против времени"
    case 13 => "Зеркальная комната"
    case 14 => "Ассоциации"
    case unknown => s"Неизвестный режим ($unknown)"
  }
}

case class Rarity(id: Int) {
  def getName: String = id match {
    case 1 => "Очень частый"
    case 2 => "Частый"
    case 3 => "Редкий"
    case 4 => "Очень редкий"
    case 5 => "Особый"
    case 10 => "Квестовый"
    case 11 => "Праздничный"
  }
}