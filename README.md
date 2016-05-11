# Flippy

Easy feature switching for scala applications

## Switch conditions

A number of default conditions are provided, and can be assembled to form a "master condition"
which represents all the conditions which go into determining if a switch is on or off based on
a set of data.

    // Only users whose names are between these fine gentlemen shall have the switch on
    val condition = StringConditions.Range("Albert", "George") on "name"

    backend.configureSwitch("is_gentleman", condition)
    backend.isActive("is_gentleman", Map("name" -> "Charlie Charlington"))  // on
    backend.isActive("is_gentleman", Map("name" -> "Kevin Louterson")  // off

You can also define your own switch condition by extending `Condition`:

    case object AllLuckySevens extends Condition {
      def appliesTo(a: Any) = a == 7777
    }

    val condition = AllLuckySevens on "HP" && Condition.Equals("Cloud") on "name"
    backend.configureSwitch("lotsofdamage", condition)
    backend.isActive("lotsofdamage", Map("name" -> "Cloud", "HP" -> 7777))  // on


## Backends

Support is currently planned for MySQL, Postgres, and Redis. At the time of writing, this support
has yet to be completed, and only an "in memory" option is available.


## Serialization

Conditions are serialized into JSON, allowing them to be stored and passed over APIs effectively.
Each condition has a core ```condition_type``` which allows the condition class and serializer to
be identified, and otherwise their properties vary based on which type of condition is present.

The structure can be nested as needed to provide complex, compound conditions. For example,
consider the JSON representation of a switch matching users who are staff members or who originate
from local host:

    {
      "condition_type": "or",
      "conditions": [
        {
          "condition_type": "namespaced",
          "attr": "ip_address",
          "fallback": false,
          "condition": {
            "condition_type": "equals",
            "value": "127.0.0.1"
          }
        },
        {
          "condition_type": "namespaced",
          "attr": "is_staff",
          "fallback": false,
          "condition": {
            "condition_type": "equals",
            "value": true
          }
        }
      ]
    }

The serialization of these conditions are delegated to subclasses of ```ConditionSerializer```,
which must define ```serialize```, ```deserialize```, and ```canSerialize``` methods to allow
the main serializer to identify the correct serializer based on ```condition_type``` and pass
serialization on to that instance. The so-called ```SerializationEngine``` is provided with a
List of appropriate ```ConditionSerializer``` subclasses in order to effect the correct
behaviour; this engine defines liftweb serialization formats, so it is intended to be used to
provide implicit formats for liftweb operations.

The default set of serializers provided for the ```SerializationEngine``` allows working with
all the default Condition types, but if you need to create your own to accompany a custom
Condition, you can roll your own serialization format, using the existing serializers as a
guide, and add them to your ```SerializationEngine```'s context by instantiating it with a
List including your own serializers. ```SerializationEngine.DEFAULTS``` is provided to easily
concatenate with your own and roll your own set of formats.

## Running tests
### Docker
To run tests involving docker, you'll need to make sure docker is installed and that you've
set the ```DOCKER_URL``` env var.

    # By default on linux
    export DOCKER_URL=unix:///var/run/docker.sock


## Management interface

HTTP user interface coming soon.
