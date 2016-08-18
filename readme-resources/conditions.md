# Condition configuration

This explains the structure of various types of conditions offered out of the box with flippy
and their JSON representation.

## Matchers

### Global on/off

The simplest conditions: always true or always false

    {
      "condition_type": "true"
    }

    {
      "condition_type": "false"
    }

### Equals

Just checks if the value equals a specific value:

    {
      "condition_type": "equals",
      "value": "Ms. Cloud"
    }

    {
      "condition_type": "equals",
      "value": 1337
    }

### String matchers
#### Range

Checks if the string is between two values alphabetically:

    {
      "condition_type": "string:range",
      "low": "abacus",
      "high: "zebra"
    }

#### Regex

Checks if the string matches a given regular expression:

    {
      "condition_type": "string:regex",
      "pattern": "^[A-Za-z0-9_]+$"
    }

#### Substring

Checks if the string contains a specific substring:

    {
      "condition_type": "string:substring",
      "value": "Mr. "
    }

## Not

Inverts the wrapped condition, eg. value should not be "error":

    {
      "condition_type": "not",
      "condition": {
        "condition_type": "equals",
        "value": "error"
      }
    }

## And / or

Combine a list of conditions with AND or OR logic. No limit on the number of conditions
to combine. eg. value must be "Tifa" or "Cloud":

    {
      "condition_type": "or",
      "conditions": [
        {
          "condition_type": "equals",
          "value": "Tifa"
        },
        {
          "condition_type": "equals",
          "value": "Cloud"
        }
      ]
    }

... or must be neither:

    {
      "condition_type": "and",
      "conditions": [
        {
          "condition_type": "not",
          "condition": {
            "condition_type": "equals",
            "value": "Tifa"
          }
        },
        {
          "condition_type": "not",
          "condition": {
            "condition_type": "equals",
            "value": "Cloud"
          }
        }
      ]
    }

## Field selection (namespacing)

Namespacing, or field selection, allows you to check a field in a set of data - since switches
are compared against data maps, you'll generally need a namespacing wrapper around any switch
condition you use.

For example, to check if your user's name is "red\_xiii":

    {
      "condition_type": "namespaced",
      "attr": "username",
      "condition": {
        "condition_type": "equals",
        "value": "red_xiii"
      }
    }

By default, if the data provided when checking a switch doesn't contain the desired field, the
condition will be assumed to be false. You can change this behaviour by providing
```fallback = true``` to the namespace wrapper. For example, if you want to decide switch state
based on a new parameter which you don't always provide, but turn the switch on if it's absent:

    {
      "condition_type": "namespaced",
      "attr": "field_may_be_missing",
      "fallback": true,
      "condition": {
        "condition_type": "equals",
        "value": "if my field is present it must be this value!"
      }
    }
