# Flippy

Easy feature switching for scala applications

## Switch conditions

A number of default conditions are provided, and can be assembled to form a "master condition"
which represents all the conditions which go into determining if a switch is on or off based on
a set of data.

    // Only users whose names are between these fine gentlemen shall have the switch on
    val condition = StringConditions.Range("Albert", "George") on "name"

    backend.configureSwitch("is\_gentleman", condition)
    backend.isActive("is\_gentleman", Map("name" -> "Charlie Charlington"))  // on
    backend.isActive("is\_gentleman", Map("name" -> "Kevin Louterson")  // off

You can also define your own switch condition by extending `Condition[T]`:

    case object AllLuckySevens extends Condition[Int] {
      def appliesTo(i: Int) = i == 7777
    }

    val condition = AllLuckySevens on "HP" && Condition.Equals("Cloud") on "name"
    backend.configureSwitch("lotsofdamage", condition)
    backend.isActive("lotsofdamage", Map("name" -> "Cloud", "HP" -> 7777))  // on


## Backends

Support is currently planned for MySQL, Postgres, and Redis. At the time of writing, this support
has yet to be completed, and only an "in memory" option is available.


## Serialisation

Coming soon, but will use Liftweb JSON.


## Management interface

HTTP user interface coming soon.
