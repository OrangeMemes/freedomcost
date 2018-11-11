package items

case class Item(id: String, title: String, picture: String, amountLimit: String,
                description: Option[String], giftLevel: Option[String],
                rooms: () => String)